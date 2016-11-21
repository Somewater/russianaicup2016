import com.sun.istack.internal.Nullable;
import model.Building;
import model.CircularUnit;
import model.Tree;
import model.Unit;

import java.awt.*;
import java.util.List;

public class Pathfinder2 extends Pathfinder {

    private final int CELL_SIZE = 10;
    private Data data;

    public void initNewTick() {
        data = null;
    }

    @Nullable
    @Override
    public P[] path(P src, P dest) {
        if (data == null)
            data = createData();
        List<Node> pathNodes =  data.nodeMap.findPath(
                (int)(src.x / CELL_SIZE),
                (int)(src.y / CELL_SIZE),
                (int)(dest.x / CELL_SIZE),
                (int)(dest.y / CELL_SIZE));

        P[] path = new P[pathNodes.size()];
        int i = 0;
        for(Node n : pathNodes) {
            double x = n.getxPosition() * CELL_SIZE;
            double y = n.getyPosition() * CELL_SIZE;
            P p = new P(x, y);
            path[i++] = p;
        }
        return path;
    }


    public void goTo(Unit target) {
        goTo(P.from(target));
    }

    public void goTo(P target) {
        goTo(path(P.from(C.self), target));
    }

    public void goTo(P[] path) {
        P p = path[1];
        Utils.goTo(p, false);
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

        int width = (int)(C.game.getMapSize() / CELL_SIZE);
        double xMax = x + r;
        double yMax = y + r;
        for (int xc = (int)(x - r); xc <= xMax; xc++)
            for (int yc = (int)(y - r); yc <= yMax; yc++) {
                if (xc >= width || yc >= width || xc < 0 || yc < 0)
                    continue;
                double dx = xc - x;
                double dy = yc - y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist - r <= 1.0) {
                    nodeMap.setWalkable(xc, yc, false);
                }
                //nodeMap.setWalkable(xc, yc, false);
            }
    }

    public void debugDrawMap() {
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
}
