import model.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class Utils {
    public static void goTo(P target, boolean canRotate) {
        C.vis.fillCircle(target.x, target.y, 5, Color.RED);
        Wizard self = C.self;
        World world = C.world;
        Game game = C.game;
        Move move = C.move;

        final int maxSpeed = 4;
        double angle = self.getAngleTo(target.x, target.y);
        if (canRotate) {
            move.setTurn(angle);
            if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
                move.setSpeed(game.getWizardForwardSpeed());
            }
        } else {
            double speed;
            double speedStrafe = game.getWizardStrafeSpeed();
            if (Math.abs(angle) < Math.PI / 2) {
                // двигаться передом
                speed = game.getWizardForwardSpeed();
            } else {
                // двигаться задом
                speed = game.getWizardBackwardSpeed();
            }
            double sin = Math.sin(self.getAngle());
            double cos = Math.cos(self.getAngle());
            P movement = new P(target.x - self.getX(), target.y - self.getY());
            P speedVecOrig = new P(speed * cos, speed * sin);
            P speedStrafeVecOrig = new P(-speedStrafe * sin, -speedStrafe * cos);
            P speedVec = movement.projectOn(speedVecOrig);
            P speedStrafeVec = movement.projectOn(speedStrafeVecOrig);
            double m = Math.min(speed + game.getWizardStrafeSpeed(), speedVec.size() + speedStrafeVec.size())
                        /
                       (speedVec.size() + speedStrafeVec.size());

            double setSpeed = speedVec.size() * m * sign(movement.dot(speedVecOrig));
            double setStrafeSpeed = speedStrafeVec.size() * m * sign(movement.dot(speedStrafeVecOrig));
            move.setSpeed(setSpeed);
            move.setStrafeSpeed(setStrafeSpeed);

//            Double Tan = Math.tan(angle);
//            if (angle == Math.PI / 2) {
//                move.setStrafeSpeed(maxSpeed);
//                move.setSpeed(0);
//            }
//            if (angle == -1* Math.PI / 2) {
//                move.setStrafeSpeed(-1*maxSpeed);
//                move.setSpeed(0);
//
//            }
//
//            double speedR = Math.abs(game.getWizardStrafeSpeed());
//            double speedB = -1*Math.abs(game.getWizardBackwardSpeed());
//            int sign = 1;
//            if (angle < 0){
//                sign = -1;
//                angle = sign * angle;
//            }
//            if((angle>Math.atan(speedR/game.getWizardForwardSpeed()))&&(angle<3*Math.PI/4)){
//                move.setStrafeSpeed(sign*speedR);
//                //if(angle > Math.PI/2)
//                move.setSpeed(Tan/speedR);
//            }
//            else{
//                double speed = speedB;
//                if(angle < Math.PI/2){
//                    speed = game.getWizardForwardSpeed();
//                }
//                move.setStrafeSpeed(sign*Math.abs(Tan*speed));
//                move.setSpeed(speed);
//
//            }

        }

    }

    static public boolean equal(double a, double b) {
        double diff = a - b;
        if (diff < 0)
            diff = -diff;
        return diff < 0.0000001;
    }

    public static double sign(double v) {
        if (v < 0) {
            return -1;
        } else if (v > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public static double padding() {
        return C.self.getRadius() /*+ C.maxSpeed + 1*/;
    }

    public static double distanceSqr(Unit u) {
        return distanceSqr(C.self, u);
    }

    public static double distanceSqr(Unit u1, Unit u2) {
        double x1 = u1.getX();
        double y1 = u1.getY();
        double x2 = u2.getX();
        double y2 = u2.getY();
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dy * dy + dx * dx;
    }

    public static Comparator<Unit> distanceCmp = new Comparator<Unit>() {
        @Override
        public int compare(Unit o1, Unit o2) {
            double d1 = distanceSqr(C.self, o1);
            double d2 = distanceSqr(C.self, o2);
            double diff = d1 - d2;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else
                return 0;
        }
    };
}
