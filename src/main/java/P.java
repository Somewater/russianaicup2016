import model.Unit;

/**
 * Point 2D, Vector 2
 */
public class P {
    public final double x;
    public final double y;

    public P(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // for usage in scala
    public static P apply(double x, double y) {
        return new P(x, y);
    }

    public double distance(double x, double y) {
        return StrictMath.hypot(this.x - x, this.y - y);
    }

    public double distanceSqr(double x, double y) {
        double dx = this.x - x;
        double dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public double distance(P p) {
        return StrictMath.hypot(this.x - p.x, this.y - p.y);
    }

    public double distanceSqr(P p) {
        double dx = this.x - p.x;
        double dy = this.y - p.y;
        return dx * dx + dy * dy;
    }

    public double distance(Unit u) {
        return StrictMath.hypot(this.x - u.getX(), this.y - u.getY());
    }

    public double distanceSqr(Unit u) {
        double dx = this.x - u.getX();
        double dy = this.y - u.getY();
        return dx * dx + dy * dy;
    }

    public P add(P p) {
        return new P(this.x + p.x, this.y + p.y);
    }

    // create vector this-that
    public P vector(P that) {
        return new P(that.x - this.x, that.y - this.y);
    }

    public P neg() {
        return new P(-x, -y);
    }

    public double size() {
        return StrictMath.sqrt(x * x + y * y);
    }

    public double sizeSqr() {
        return x * x + y * y;
    }

    public P copy() {
        return new P(this.x, this.y);
    }

    public static P from(Unit u) {
        return new P(u.getX(), u.getY());
    }

    public double dot(P that) {
        return this.x * that.x + this.y * that.y;
    }

    public P mult(double m) {
        return new P(m * x, m * y);
    }

    public P norm() {
        return mult(1.0 / size());
    }

    public P projectOn(P other) {
        P n = other.norm();
        return n.mult(n.dot(this));
    }

    public P nearest(P p1, P p2) {
        double d1 = p1.distanceSqr(this);
        double d2 = p2.distanceSqr(this);
        if (d1 < d2)
            return p1;
        else
            return p2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof P) {
            P p = (P) obj;
            return Utils.equal(this.x, p.x) && Utils.equal(this.y, p.y);
        } else
            return super.equals(obj);
    }

    public static P[] intersectionLineCircle(P l1, P l2, P center, double r) {
//        double m = (l2.y - l1.y) / (l2.x - l1.x);
//        double b = l1.y - c.y - m * (l1.x - c.x);
//        double m2 = m*m;
//        double underRadical = r*r * m2 + r*r - b*b;
//        if (underRadical < 0) {
//            return null;
//        } else if (underRadical == 0) {
//            return null; // tangent
//        } else {
//            double sqrt = StrictMath.sqrt(underRadical);
//            double t1 = (-m*b + sqrt)/(m2 + 1);
//            double t2 = (-m*b - sqrt)/(m2 + 1);
//            P p1 = new P(t1 + c.x, m * t1 + b + c.y);
//            P p2 = new P(t2 + c.x, m * t2 + b + c.y);
//            return new P[]{p1, p2};
//        }

        double baX = l2.x - l1.x;
        double baY = l2.y - l1.y;
        double caX = center.x - l1.x;
        double caY = center.y - l1.y;

        double a = baX * baX + baY * baY;
        double bBy2 = baX * caX + baY * caY;
        double c = caX * caX + caY * caY - r * r;

        double pBy2 = bBy2 / a;
        double q = c / a;

        double disc = pBy2 * pBy2 - q;
        if (disc <= 0) {
            return null;
        }
        // if disc == 0 ... dealt with later
        double tmpSqrt = Math.sqrt(disc);
        double abScalingFactor1 = -pBy2 + tmpSqrt;
        double abScalingFactor2 = -pBy2 - tmpSqrt;

        P p1 = new P(l1.x - baX * abScalingFactor1, l1.y
                - baY * abScalingFactor1);
        P p2 = new P(l1.x - baX * abScalingFactor2, l1.y
                - baY * abScalingFactor2);
        double size = l1.distance(l2);
        boolean p1InLine = Utils.equal(size, l1.distance(p1) + p1.distance(l2));
        boolean p2InLine = Utils.equal(size, l1.distance(p2) + p2.distance(l2));
        if (p1InLine && p2InLine) {
            return new P[]{p1, p2};
        } else if (p1InLine || p2InLine) {
            return new P[]{p1, p2};

//            if (p2InLine) {
//                return new P[]{p1.nearest(l1, l2), p2};
//            } else {
//                return new P[]{p1, p2.nearest(l1, l2)};
//            }
        } else {
            return null;
        }
    }


    public static P[] intersectionCircles(P c1, double r1, P c2, double r2) {
        double dx = c1.x - c2.x;
        double dy = c1.y - c2.y;
        double d2 = dx * dx + dy * dy;
        double dr = r1 - r2;
        if (r1 * r1 + r2 * r2 < d2) {
            return null; // no intersection
        } else if (d2 < dr * dr) {
            return null; // one into another
        } else {
            double d = StrictMath.sqrt(d2);
            double a = r1 * r1 - r2 * r2 + d2 / (2.0 * d);

            double x2 = c1.x + dx * a / d;
            double y2 = c1.y + dy * a / d;

            double h = StrictMath.sqrt(r1 * r1 - a * a);
            double rx = (-dy) * (h / d);
            double ry = dx * (h / d);

            P p1 = new P(c2.x + rx, c2.x + ry);
            P p2 = new P(c2.x - rx, c2.x - ry);

            return new P[]{p1, p2};
        }
    }

    // tangent from current point to circle
    public P[] tangent(P c, double r) {
        double dx = c.x - x;
        double dy = c.y - y;
        double dd = StrictMath.sqrt(dx * dx + dy * dy);
        double a = StrictMath.asin(r / dd);
        double b = StrictMath.atan2(dy, dx);
        double t1 = b - a;
        double t2 = b + a;
        P p1 = new P(r * StrictMath.sin(t1), r * -StrictMath.cos(t1));
        P p2 = new P(r * -StrictMath.sin(t2), r * StrictMath.cos(t2));
        return new P[]{p1, p2};
    }

    // perpendicular from point "p" to line "l1 -l2"
    public static P perpendicular(P v1, P v2, P p) {
        P e1 = new P(v2.x - v1.x, v2.y - v1.y);
        P e2 = new P(p.x - v1.x, p.y - v1.y);
        double valDp = e1.dot(e2);
        // get length of vectors
        double lenLineE1 = Math.sqrt(e1.x * e1.x + e1.y * e1.y);
        double lenLineE2 = Math.sqrt(e2.x * e2.x + e2.y * e2.y);
        double cos = valDp / (lenLineE1 * lenLineE2);
        // length of v1P'
        double projLenOfLine = cos * lenLineE2;
        return new P(v1.x + (projLenOfLine * e1.x) / lenLineE1, v1.y + (projLenOfLine * e1.y) / lenLineE1);
    }

    public static boolean onLine(P l1, P l2, P p) {
        final double precision = 3.0;
        if (!Utils.equal(l1.x, l2.x)) {
            if (l1.x > l2.x) {
                P buf = l1;
                l1 = l2;
                l2 = buf;
            }

            double m = (l2.y - l1.y) / (l2.x - l1.x);
            double b = l1.y - m * l1.x;

            double py = p.x * m + b;
            return Utils.equal(p.y, py, precision) && l1.x < p.x && p.x < l2.x;
        } else {
            return Utils.equal(l1.distance(p) + p.distance(l2), l1.distance(l2), precision);
        }
    }

    public static boolean perpendicularOnLine(P l1, P l2, P p) {
        P perpend = P.perpendicular(l1, l2, p);
        return P.onLine(l1, l2, perpend);
    }
}
