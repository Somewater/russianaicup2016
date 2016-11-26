import java.util.*;

/**
 * Simple memory optimized A* pathfinder.
 * Usage:
 * <pre>
 *     AMaze a = new AMaze(width, height);
 * <pre/>
 */
public class AMaze<UserData> {
    protected final int width;
    protected final int height;
    protected final Node<UserData>[] nodes;

    public boolean defaultWalkable = true;
    public double defaultMovementPenalty = 1.0D;
    public double diagonalPenalty = 1.4;
    public boolean canMoveDiagonaly = true;
    public int maxIterationsForSearch = 1000;

    // list containing nodes not visited but adjacent to visited nodes.
    protected PriorityQueue<NodeRef<UserData>> openList;
    // list containing nodes already visited/taken care of.
    protected Set<Integer> closedList;
    // done finding path?
    protected boolean done = false;

    public Comparator<NodeRef<UserData>> priorityComparator = new Comparator<NodeRef<UserData>>() {
        @Override
        public int compare(NodeRef<UserData> o1, NodeRef<UserData> o2) {
            double diff = (o1.gCost + o1.hCost) - (o2.gCost + o2.hCost);
            if (diff < 0) return -1;
            else if (diff > 0) return 1;
            else return 0;
        }
    };

    public AMaze(int width, int height) {
        this.width = width;
        this.height = height;
        nodes = new Node[width * height];
    }

    public void drawMap() {
        for (int i = 0; i <= width; i++) {
            System.out.print(" _"); // boarder of nodeMap
        }
        System.out.print("\n");

        for (int y = height - 1; y >= 0; y--) {
            System.out.print("|"); // boarder of nodeMap
            for (int x = 0; x < width; x++) {
                int idx =  x * width + y;
                Node node = nodes[idx];

                if (node == null ? defaultWalkable : node.walkable) {
                    if (node != null && node.movementPenalty > defaultMovementPenalty) {
                        System.out.print(" .");
                    } else {
                        System.out.print("  ");
                    }
                } else {
                    System.out.print(" #"); // draw unwakable
                }
            }
            System.out.print("|\n"); // boarder of nodeMap
        }

        for (int i = 0; i < width; i++) {
            System.out.print(" _"); // boarder of nodeMap
        }
    }

    public void setWalkable(int x, int y, boolean walkable, UserData userData) {
        int idx = x * width + y;
        Node node = nodes[idx];
        if (node == null) {
            node = new Node(userData);
            node.walkable = walkable;
            node.movementPenalty = defaultMovementPenalty;
            nodes[idx] = node;
        } else {
            node.walkable = walkable;
        }
    }

    public void setMovementPenalty(int x, int y, double movementPenalty, UserData userData) {
        int idx = x * width + y;
        Node node = nodes[idx];
        if (node == null) {
            node = new Node(userData);
            node.walkable = defaultWalkable;
            node.movementPenalty = movementPenalty;
            nodes[idx] = node;
        } else {
            node.movementPenalty = movementPenalty;
        }
    }

    public void setUserData(int x, int y, UserData userData) {
        int idx = x * width + y;
        Node node = nodes[idx];
        if (node == null || node.userData != userData) {
            node = new Node(userData);
            node.walkable = defaultWalkable;
            node.movementPenalty = defaultMovementPenalty;
            nodes[idx] = node;
        }
    }

    public Node<UserData> getNode(int x, int y) {
        int idx = x * width + y;
        return nodes[idx];
    }

    public final NodeRef<UserData>[] findPath(int oldX, int oldY, int newX, int newY) {
        return findPath(oldX, oldY, newX, newY, -1);
    }

    public final NodeRef<UserData>[] findPath(int srcX, int srcY, int destX, int destY, double distance) {
        Node destNode = nodes[destX * width + destY];
        if (destNode != null && distance <= 0 && !destNode.walkable)
            return null;
        NodeRef<UserData> destNodeRef = new NodeRef<>(destX, destY, destNode);

        openList = new PriorityQueue<>(priorityComparator);
        closedList = new HashSet<>();
        NodeRef srcNodeRef = new NodeRef(srcX, srcY, nodes[srcX * width + srcY]);
        openList.add(srcNodeRef); // add starting node to open list
        double distanceSqr = distance > 0 ? distance * distance : -1;

        done = false;
        NodeRef current;
        int counter = 0;

        long maxMs = Math.max(10, Math.min(500, C.tickDurationAvailable - C.tickDurationSum - 100));
        while (canContinue(counter++)) {
            current = lowestFInOpen(); // get node with lowest fCosts from openList
            closedList.add(current.x * width + current.y); // add current node to closed list
            openList.remove(current); // delete current node from open list

            if ((current.x == destX)
                    && (current.y == destY)) { // found goal
                clearTmpData();
                return calcPath(srcNodeRef, current);
            }

            if (distanceSqr > 0) {
                double dx = current.x - destX;
                double dy = current.y - destY;
                double dist2 = dx * dx + dy * dy;
                if (dist2 <= distanceSqr) {
                    clearTmpData();
                    return calcPath(srcNodeRef, current);
                }
            }

            // for all adjacent nodes:
            List<NodeRef<UserData>> adjacentNodes = getAdjacent(current);
            for (int i = 0; i < adjacentNodes.size(); i++) {
                NodeRef currentAdj = adjacentNodes.get(i);
                if (!openList.contains(currentAdj)) { // node is not in openList
                    currentAdj.previous = current; // set current node as previous for this node
                    currentAdj.hCost = calcHCost(current, destNodeRef); // set h costs of this node (estimated costs to goal)
                    currentAdj.gCost = calcGCost(current, currentAdj); // set g costs of this node (costs from start to this node)
                    openList.add(currentAdj); // add node to openList
                } else { // node is in openList
                    double currentGCost = calcGCost(current, currentAdj);
                    if (currentAdj.gCost > currentGCost) { // costs from current node are cheaper than previous costs
                        currentAdj.previous = current; // set current node as previous for this node
                        currentAdj.gCost = currentGCost; // set g costs of this node (costs from start to this node)
                    }
                }
            }

            if (openList.isEmpty()) { // no path exists
                clearTmpData();
                return null;
            }
        }
        clearTmpData();
        return null; // unreachable
    }

    protected boolean canContinue(int counter) {
        return counter < maxIterationsForSearch;
    }

    protected void clearTmpData() {
        openList = null;
        closedList = null;
    }

    protected NodeRef lowestFInOpen() {
        return openList.remove();
    }

    protected double calcHCost(NodeRef cur, NodeRef dest) {
        int dx = cur.x - dest.x;
        int dy = cur.y - dest.y;
        if (dx < 0) dx = -dx;
        if (dy < 0) dy = -dy;
        return dx + dy;
    }

    protected double calcGCost(NodeRef prev, NodeRef cur) {
        double movementPenalty = (cur.node == null ? defaultMovementPenalty : cur.node.movementPenalty);
        if (cur.diagonal)
            movementPenalty *= diagonalPenalty;
        return prev.gCost + movementPenalty;
    }

    /**
     * calculates the found path between two points according to
     * their given <code>previousNode</code> field.
     *
     * @param start
     * @param goal
     * @return
     */
    protected NodeRef<UserData>[] calcPath(NodeRef<UserData> start, NodeRef<UserData> goal) {
        // TODO if invalid nodes are given (eg cannot find from
        // goal to start, this method will result in an infinite loop!)
        LinkedList<NodeRef<UserData>> path = new LinkedList<>();

        NodeRef<UserData> curr = goal;
        boolean done = false;
        while (!done) {
            path.addFirst(curr);
            curr = curr.previous;

            if (curr == null || curr.equals(start)) {
                done = true;
            }
        }
        if (path.size() == 1)
            path.addFirst(start);
        return path.toArray(new NodeRef[path.size()]);
    }

    /**
     * returns a LinkedList with nodes adjacent to the given node.
     * if those exist, are walkable and are not already in the closedList!
     */
    protected List<NodeRef<UserData>> getAdjacent(NodeRef node) {
        int x = node.x;
        int y = node.y;
        List<NodeRef<UserData>> adj = new LinkedList<>();

        Node<UserData> temp = null;
        if (x > 0) {
            int idx = (x - 1) * width + y;
            temp = nodes[idx];
            if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                adj.add(new NodeRef(x - 1, y, temp));
            }
        }

        if (x < width - 1) {
            int idx = (x + 1) * width + y;
            try {
                temp = nodes[idx];
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                adj.add(new NodeRef(x + 1, y, temp));
            }
        }

        if (y > 0) {
            int idx = x * width + (y - 1);
            temp = nodes[idx];
            if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                adj.add(new NodeRef(x, y - 1, temp));
            }
        }

        if (y < height - 1) {
            int idx = x * width + (y + 1);
            temp = nodes[idx];
            if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                adj.add(new NodeRef(x, y + 1, temp));
            }
        }

        // add nodes that are diagonaly adjacent too:
        if (canMoveDiagonaly) {
            if (x < width - 1 && y < height - 1) {
                int idx = (x + 1) * width + (y + 1);
                temp = nodes[idx];
                if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                    adj.add(new NodeRef(x + 1, y + 1, temp, true));
                }
            }

            if (x > 0 && y > 0) {
                int idx = (x - 1) * width + (y - 1);
                temp = nodes[idx];
                if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                    adj.add(new NodeRef(x - 1, y - 1, temp, true));
                }
            }

            if (x > 0 && y < height - 1) {
                int idx = (x - 1) * width + (y + 1);
                temp = nodes[idx];
                if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                    adj.add(new NodeRef(x - 1, y + 1, temp, true));
                }
            }

            if (x < width - 1 && y > 0) {
                int idx = (x + 1) * width + (y - 1);
                temp = nodes[idx];
                if ((temp == null ? defaultWalkable : temp.walkable) && !closedList.contains(idx)) {
                    adj.add(new NodeRef(x + 1, y - 1, temp, true));
                }
            }
        }
        return adj;
    }

    public static class Node<UserData> {
        public final UserData userData;
        public boolean walkable;
        public double movementPenalty;

        public Node() {
            userData = null;
        }

        public Node(UserData userData) {
            this.userData = userData;
        }
    }

    public static class NodeRef<UserData> {
        public final int x;
        public final int y;
        public final Node<UserData> node;
        public final boolean diagonal;

        public double hCost;
        public double gCost;
        public NodeRef previous;

        public NodeRef(int x, int y, Node node, boolean diagonal) {
            this.x = x;
            this.y = y;
            this.node = node;
            this.diagonal = diagonal;
        }

        public NodeRef(int x, int y, Node node) {
            this.x = x;
            this.y = y;
            this.node = node;
            this.diagonal = false;
        }

        public UserData userData() {
            if (node == null)
                return null;
            else
                return node.userData;
        }
    }
}
