import model.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Pathfinder2 implements IPathfinder {

    private static final int DEFAULT_SET_SIZE = 50;

    public static int CELL_SIZE = DEFAULT_SET_SIZE;
    private Data data;
    private P self;
    private LinkedList<PathCache> cache = new LinkedList<>();

    public void initNewTick() {
        data = null;
        P newSelf = P.from(C.self);
        if (self != null && self.equals(newSelf))
            C.pathfinderFailureTicks++;
        else
            C.pathfinderFailureTicks = 0;
        self = newSelf;
        if (C.pathfinderFailureTicks > 10) {
            if (CELL_SIZE > 5) {
                CELL_SIZE -= 5;
            } else {
                CELL_SIZE = DEFAULT_SET_SIZE;
            }
        } else
            CELL_SIZE = DEFAULT_SET_SIZE;

        if (cache.size() > 100) {
            cache.removeFirst();
        }
        Iterator<PathCache> iter = cache.iterator();
        while (iter.hasNext()) {
            PathCache c = iter.next();
            if (C.world.getTickIndex() - c.tick > 10)
                iter.remove();
        }
    }

    @Override
    public P[] path(P src, P dest) {
        return  path(src, dest, -1);
    }

    public P[] path(P src, P dest, double radius) {
        P[] path;
        path = tryFindShortPath(src, dest, radius);
        if (path != null)
            return path;

        path = tryFindCachedPath(src, dest, radius);
        if (path != null)
            return path;

        if (data == null)
            data = createData();
        List<Node> pathNodes =  data.nodeMap.findPath(
                (int)(src.x / CELL_SIZE),
                (int)(src.y / CELL_SIZE),
                (int)(dest.x / CELL_SIZE),
                (int)(dest.y / CELL_SIZE),
                radius / CELL_SIZE);

        if (pathNodes == null)
            return null;

        path = new P[pathNodes.size()];
        int i = 0;
        for(Node n : pathNodes) {
            double x = n.getxPosition() * CELL_SIZE;
            double y = n.getyPosition() * CELL_SIZE;
            P p = new P(x, y);
            path[i++] = p;
        }

        if (path.length > 20)
            cachePath(src, dest, radius, path);
        C.pathFromCache = 0;
        return path;
    }


    public boolean goTo(Unit target) {
        double radius = -1;
        if (target instanceof CircularUnit) {
            radius = ((CircularUnit) target).getRadius();
            radius += Utils.padding();
            radius += C.game.getStaffRange() / 2;
        }
        return goTo(path(self, P.from(target), radius));
    }

    public boolean goTo(P target) {
        return goTo(target, -1);
    }

    public boolean goTo(P target, double radius) {
        P[] path = path(self, target);
        if (path == null)
            path = path(self, target, radius);
        return goTo(path);
    }

    public boolean goTo(P[] path) {
        if (path == null)
            return false;
        Utils.drawPath(path);
        P p;
        if (P.onLine(path[0], path[1], self) || P.perpendicularOnLine(path[0], path[1], self)) {
            p = path[1];
        } else {
            p = path[0];
        }
        Utils.goTo(p, false);
        return true;
    }

    private void cachePath(P src, P dest, double radius, P[] path) {
        PathCache c = new PathCache(src, dest, radius, path, C.world.getTickIndex());
        cache.add(c);
    }

    private P[] tryFindShortPath(P src, P dest, double radius) {
        if (src.distance(dest) < CELL_SIZE) {
            return new P[]{src, dest};
        }
        return null;
    }

    private P[] tryFindCachedPath(P src, P dest, double radius) {
        Iterator<PathCache> iter = cache.descendingIterator();
        while (iter.hasNext()) {
            PathCache c = iter.next();
            if (c.radius == radius && c.dest.equals(dest)) {
                double srcDistance2 = c.src.distanceSqr(src);
                if (srcDistance2 < 100 * 100) {
                    int shortLen = c.path.length;
                    if (shortLen > 10) shortLen = 10;
                    for (int i = 1; i < shortLen; i++) {
                        P p1 = c.path[i - 1];
                        P p2 = c.path[i];
                        boolean ok = false;
                        if (P.onLine(p1, p2, src))
                            ok = true;
                        if (!ok) {
                            ok = P.perpendicularOnLine(p1, p2, src);
                        }
                        if (ok) {
                            C.pathFromCache++;
                            P[] path = new P[c.path.length + 1 - i];
                            System.arraycopy(c.path, i - 1, path, 0, path.length);
                            return path;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Data createData() {
        Data d = new Data();
        d.nodeMap = new NodeMap((int)(C.game.getMapSize() / CELL_SIZE),
                (int)(C.game.getMapSize() / CELL_SIZE),
                new NodeFact());
        for (Tree t : C.world.getTrees())
            closeNodes(d.nodeMap, t);
        for (Building b : C.world.getBuildings())
            closeNodes(d.nodeMap, b);
        for (Wizard w : C.world.getWizards())
            if (!w.isMe() && Utils.distanceSqr(w) < 500*500)
                closeNodes(d.nodeMap, w);
        for (Minion m : C.world.getMinions())
            if (Utils.distanceSqr(m) < 500*500)
                closeNodes(d.nodeMap, m);
        return d;
    }

    private static class Data {
        NodeMap<Node> nodeMap;
    }

    private static class NodeFact implements NodeFactory {

        @Override
        public AbstractNode createNode(int x, int y) {
            return new Node(x, y);
        }
    }

    private static class Node extends AbstractNode {

        public Node(int xPosition, int yPosition) {
            super(xPosition, yPosition);
        }

        public void sethCosts(AbstractNode endNode) {
            this.sethCosts((absolute(this.getxPosition() - endNode.getxPosition())
                    + absolute(this.getyPosition() - endNode.getyPosition()))
                    * BASICMOVEMENTCOST);
        }

        private int absolute(int a) {
            return a > 0 ? a : -a;
        }
    }

    private void closeNodes(NodeMap nodeMap, CircularUnit u) {
        double x = u.getX() / CELL_SIZE;
        double y = u.getY() / CELL_SIZE;
        double r = (u.getRadius() + Utils.padding()) / CELL_SIZE;
        double r2 = r * r;
        double selfX = self.x / CELL_SIZE;
        double selfY = self.y / CELL_SIZE;

        int width = (int)(C.game.getMapSize() / CELL_SIZE);
        double xMax = x + r;
        double yMax = y + r;
        for (int xc_0 = (int)(x - r); xc_0 <= xMax; xc_0++)
            for (int yc_0 = (int)(y - r); yc_0 <= yMax; yc_0++) {
                if (xc_0 >= width || yc_0 >= width || xc_0 < 0 || yc_0 < 0)
                    continue;

                // check 4 corners
                boolean walkable = true;
                cycle:
                for (int xc_d = 0; xc_d < 2; xc_d++) {
                    for (int yc_d = 0; yc_d < 2; yc_d++) {
                        double xc = xc_0 + xc_d;
                        double yc = yc_0 + yc_d;
                        double dx = xc - x;
                        double dy = yc - y;
                        double dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist - r <= 0.0) {
                            if (Math.abs(xc - selfX) > 1.0 || Math.abs(yc - selfY) > 1.0) {
                                walkable = false;
                                break cycle;
                            }
                        }
                    }
                }

                if (!walkable)
                    nodeMap.setWalkable(xc_0, yc_0, false);
            }
    }

    public void debugDrawMap() {
        if (!C.debug)
            return;
        if (data == null)
            return;
        for (int x = 0; x < data.nodeMap.width; x++) {
            for (int y = 0; y < data.nodeMap.higth; y++) {
                Node n = data.nodeMap.getNode(x, y);
                if (!n.isWalkable()) {
                    double nx = n.getxPosition() * CELL_SIZE;
                    double ny = n.getyPosition() * CELL_SIZE;
                    double nxMax = nx + CELL_SIZE;
                    double nyMax = ny + CELL_SIZE;
                    C.vis.rect(nx, ny, nxMax, nyMax, Color.cyan);
                }
            }
        }
    }

    private static class PathCache {

        public final P src;
        public final P dest;
        public final double radius;
        public final P[] path;
        public final int tick;

        public PathCache(P src, P dest, double radius, P[] path, int tick) {
            this.src = src;
            this.dest = dest;
            this.radius = radius;
            this.path = path;
            this.tick = tick;
        }
    }
}
