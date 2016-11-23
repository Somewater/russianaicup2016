import model.Building;
import model.CircularUnit;
import model.Tree;
import model.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Pathfinder implements IPathfinder {

    private Data data;
    private P self;

    public void initNewTick() {
        data = null;
        self = null;
    }

    public double distance(Unit target) {
        if (self == null)
            self = P.from(C.self);
        return distance(P.from(target), self);
    }

    public double distance(P src, P dest) {
        P[] path = path(src, dest);
        if (path == null) {
            return -1;
        } else {
            double sum = 0D;
            P prev = null;
            for (P p : path) {
                if (prev != null) {
                    sum += p.distance(prev);
                }
                prev = p;
            }
            return sum;
        }
    }

    public P[] path(P src, P dest) {
        if (data == null)
            data = createData();

        ArrayList<Triple<Wall, Wall[], P[]>> intersections = new ArrayList<>();
        for (Wall[] ws: data.walls) {
            for (Wall w : ws) {
                P[] inters = w.intersectsWithLine(src, dest);
                if (inters != null)
                    intersections.add(new Triple<>(w, ws, inters));
            }
        }

        if (intersections.isEmpty()) {
            return new P[]{src, dest};
        } else {
            sortInteractions(intersections, src, dest);
            ArrayList<P> result = new ArrayList<>();
            result.add(src);
            P prevRes = src;
            P prev = src;
            for (int i = 0; i < intersections.size(); i++) {
                P next;
                if (i + 1 < intersections.size()) {
                    next = intersections.get(i + 1).c[0];
                } else {
                    next = dest;
                }
                Triple<Wall, Wall[], P[]> inter = intersections.get(i);
                P[] subpath = wallIntersectToPath(prev, next, inter.a, inter.b, inter.c);
                for (P p : subpath) {
                    if (!prevRes.equals(p)) {
                        result.add(p);
                        prevRes = p;
                    }
                }
                prev = inter.c[1];
            }
            result.add(dest);
            return result.toArray(new P[result.size()]);
        }
    }

    private void sortInteractions(ArrayList<Triple<Pathfinder.Wall, Pathfinder.Wall[], P[]>> interactions, P src, P dest) {
        boolean horizontal = false;
        int direct = 0;
        if (Utils.equal(src.x, dest.x)) {
            direct = src.y < dest.y ? 1 : -1;
        } else {
            horizontal = true;
            direct = src.x < dest.x ? 1 : -1;
        }
        boolean finalHorizontal = horizontal;
        int finalDirect = direct;
        Collections.sort(interactions, new Comparator<Triple<Wall, Wall[], P[]>>() {
            @Override
            public int compare(Triple<Wall, Wall[], P[]> o1, Triple<Wall, Wall[], P[]> o2) {
                double diff;
                if (finalHorizontal) {
                    diff = o1.c[0].x - o2.c[0].x;
                } else {
                    diff = o1.c[0].y - o2.c[0].y;
                }
                if (diff < 0) {
                    return -finalDirect;
                } else if (diff > 0) {
                    return finalDirect;
                } else {
                    return 0;
                }
            }
        });
    }

    public boolean goTo(Unit target) {
        return goTo(P.from(target));
    }

    public boolean goTo(P target, double radius) {
        // ignore radius
        return goTo(path(P.from(C.self), target));
    }

    public boolean goTo(P target) {
        return goTo(path(P.from(C.self), target));
    }

    public boolean goTo(P[] path) {
        P p = path[1];
        if (path.length > 2) {
            P p2 = path[2];
            if (p.distanceSqr(C.self) < C.maxSpeed * C.maxSpeed)
                p = p2;
        }
        Utils.goTo(p, false);
        return true;
    }

    private Data createData() {
        Data d = new Data();
        int i = 0;
        Wall[] walls = new Wall[C.world.getTrees().length + C.world.getBuildings().length];
        for (Tree t : C.world.getTrees())
            walls[i++] = Wall.from(t);
        for (Building b : C.world.getBuildings())
            walls[i++] = Wall.from(b);

        ArrayList<ArrayList<Wall>> wallSets = new ArrayList<>();
        for (i = 0; i < walls.length; i++) {
            Wall w = walls[i];
            if (w != null) {
                ArrayList<Wall> set = new ArrayList<>();
                set.add(w);
                wallToWallSets(w, i, walls, set);
                wallSets.add(set);
            }
        }

        d.walls = new Wall[wallSets.size()][];
        i = 0;
        for (ArrayList<Wall> ws : wallSets)
            d.walls[i++] = ws.toArray(new Wall[ws.size()]);
        return d;
    }

    private void wallToWallSets(Wall w, int fromPos, Wall[] walls, ArrayList<Wall> result) {
        for (int i = fromPos; i < walls.length; i++) {
            Wall w2 = walls[i];
            if (w2 == null || w2 == w)
                continue;
            if (w2.intersects(w)) {
                walls[i] = null;
                wallToWallSets(w2, i + 1, walls, result);
                result.add(w2);
            }
        }
    }

    private static class Data {
        Wall[][] walls;
    }

    private static class Wall extends P {
        public final double r;
        public final Unit unit;

        public Wall(double x, double y, double r) {
            super(x, y);
            this.r = r;
            unit = null;
        }

        public Wall(double x, double y, double r, Unit unit) {
            super(x, y);
            this.r = r;
            this.unit = unit;
        }
        public static Wall from(CircularUnit u) {
            return new Wall(u.getX(), u.getY(), u.getRadius() + Utils.padding(), u);
        }

        public P[] intersectsWithLine(P start, P end) {
            return P.intersectionLineCircle(start, end, this, r);
        }



        public boolean intersects(Wall that) {
            double distSqr = this.distanceSqr(that);
            double r = this.r + that.r;
            return distSqr <= r * r;
        }
    }

    private P[] wallIntersectToPath(P start, P end, Wall w, Wall[] ws, P[] intersections) {
        if (intersections.length < 2) {
            return new P[]{start, intersections[0]};
        }

        // TODO: process wall sets
        P v1 = intersections[0];
        P v2 = intersections[1];

        P cProj = P.perpendicular(v1, v2, w);

        P vec = w.vector(cProj);
        vec = vec.add(vec).norm().mult(w.r - vec.size());

        P p1 = v1.add(vec);
        P p2 = v2.add(vec);

        return new P[]{ v1, p1, p2, v2 };
    }
}
