import model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class Controller {
    private final int NEAREST_RADIUS = 180;
    private LaneType lane = null;
    private Map<LaneType, P[]> waypointsByLane = null;
    private P[] waypoints;
    private P[] waypointsReversed;
    private int nextPointBranchCooldown = 0;

    public void move() {
        if (waypointsByLane == null)
            createWaypoints();
        selectLane();

        boolean move = setMove();
        boolean attack = setAttack();

        if (!move && C.pathfinderFailureTicks > 0 && !attack) {
            // посохом пробьём себе дорогу
            double rotation;
            if ((C.pathfinderFailureTicks / 100) % 2 == 0)
                rotation = C.game.getWizardMaxTurnAngle();
            else
                rotation = -C.game.getWizardMaxTurnAngle();
            C.move.setTurn(rotation);
            C.move.setAction(ActionType.STAFF);
        }
    }

    private boolean setMove() {
        boolean move = false;
        if (!C.targets.bonuses.isEmpty() && Utils.distanceSqr(C.targets.bonuses.get(0)) < 500 * 500) {
            move = C.pathfinder.goTo(C.targets.bonuses.get(0));
            if (move) Utils.debugMoveTarget("bonus", C.targets.bonuses.get(0), null);
        }


        if (!move) {
            P nextWaypoint = getNextWaypoint();
            P previousWaypoint = getPreviousWaypoint();
            if (C.health() > 0.7) {
                // идти в бой
                if (!C.targets.enemies.isEmpty()) {
                    move = C.pathfinder.goTo(C.targets.enemies.get(0));
                    if (move) Utils.debugMoveTarget("attack_enemy", C.targets.enemies.get(0), null);
                } else {
                    move = C.pathfinder.goTo(nextWaypoint, NEAREST_RADIUS);
                    if (move) Utils.debugMoveTarget("next_waypoint", null, nextWaypoint);
                }
            } else if (C.health() > 0.4) {
                // по возможности уклоняться
                nextPointBranchCooldown--;
                if (C.targets.dangers.isEmpty() && nextPointBranchCooldown <= 0)
                    move = C.pathfinder.goTo(nextWaypoint, NEAREST_RADIUS);
                    if (move) Utils.debugMoveTarget("next_waypoint", null, nextWaypoint);
                else {
                    nextPointBranchCooldown = 100;
                    move = C.pathfinder.goTo(getPreviousWaypoint(), NEAREST_RADIUS);
                        if (move) Utils.debugMoveTarget("previous_waypoint", null, previousWaypoint);
                }
            } else {
                // уклоняться и лечиться
                move = C.pathfinder.goTo(getPreviousWaypoint(), NEAREST_RADIUS);
                if (move) Utils.debugMoveTarget("previous_waypoint", null, previousWaypoint);
            }
        }

        if (!move) {
            // паффайндинг не паффайндится :(
            C.pathfinderFailureTicks++;
            Utils.debugMoveTarget("PATHFINDING_FAILURE", null, null);
        }

        return move;
    }

    private boolean setAttack() {
        LivingUnit victim = null;
        if (!C.targets.enemies.isEmpty())
            victim = C.targets.enemies.get(0);
        if (victim != null) {
            double distance = C.self.getDistanceTo(victim);

            if (distance <= C.self.getCastRange()) {
                double angle = C.self.getAngleTo(victim);
                C.move.setTurn(angle);

                if (StrictMath.abs(angle) < C.game.getStaffSector() / 2.0D) {
                    int[] cooldowns = C.self.getRemainingCooldownTicksByAction();
                    if (distance <= C.game.getStaffRange() && cooldowns[ActionType.STAFF.ordinal()] <= 0) {
                        C.move.setAction(ActionType.STAFF);
                        return true;
                    } else if (cooldowns[ActionType.MAGIC_MISSILE.ordinal()] <= 0) {
                        C.move.setAction(ActionType.MAGIC_MISSILE);
                        C.move.setCastAngle(angle);
                        C.move.setMinCastDistance(distance - victim.getRadius() + C.game.getMagicMissileRadius());
                        return true;
                    } else if (cooldowns[ActionType.FROST_BOLT.ordinal()] <= 0 && C.mana() > 0.5) {
                        C.move.setAction(ActionType.FROST_BOLT);
                        C.move.setCastAngle(angle);
                        C.move.setMinCastDistance(distance - victim.getRadius() + C.game.getMagicMissileRadius());
                        return true;
                    } else if (cooldowns[ActionType.FIREBALL.ordinal()] <= 0 && C.mana() > 0.7) {
                        C.move.setAction(ActionType.FIREBALL);
                        C.move.setCastAngle(angle);
                        C.move.setMinCastDistance(distance - victim.getRadius() + C.game.getMagicMissileRadius());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void selectLane() {
        Message[] messages = C.self.getMessages();
        if (messages.length > 0) {
            if (messages[0].getLane() != null) {
                lane = messages[0].getLane();
                waypoints = waypointsByLane.get(lane);
                waypointsReversed = getWaypointsReversed();
            }
        }
        if (lane == null) {
            switch (C.random.nextInt(3)) {
                case 0:
                    lane = LaneType.TOP;
                    break;
                case 1:
                    lane = LaneType.MIDDLE;
                    break;
                case 2:
                    lane = LaneType.BOTTOM;
                    break;
            }

            waypoints = waypointsByLane.get(lane);
            waypointsReversed = getWaypointsReversed();
        }
    }
    
    private void createWaypoints() {
        double mapSize = C.game.getMapSize();
        waypointsByLane =  new EnumMap<>(LaneType.class);
        waypointsByLane.put(LaneType.MIDDLE, new P[]{
                new P(1072, 2925),
                new P(1785, 2244),
                new P(2000, 2000),// attack line
                new P(2214, 1755),// <- after I tower destruction
                new P(2483, 1497),
                new P(2678, 1313),// attack line
                new P(2927, 1074),// <- after II tower destruction
                new P(3353, 639)
        });

        waypointsByLane.put(LaneType.TOP, new P[]{
                new P(208, 2693),
                new P(175, 1656),
                new P(183, 556),
                new P(556, 183),
                new P(1028, 183),
                new P(1300, 183), // attack line
                new P(1688, 208), // <- after I tower destruction
                new P(1968, 183),
                new P(2253, 183), // attack line
                new P(2630, 175), // <- after II tower destruction
                new P(3264, 183)
        });

        waypointsByLane.put(LaneType.BOTTOM, new P[]{
                new P(1370, 3825),
                new P(2312, 3791),
                new P(3443, 3816),
                new P(3816, 3443),
                new P(3816, 3017),
                new P(3816, 2738), // attack line
                new P(3825, 2344),// <- after I tower destruction
                new P(3816, 1985),
                new P(3816, 1694), // attack line
                new P(3791, 1307),// <- after II tower destruction
                new P(3816, 735)
        });
    }

    private P getNextWaypoint() {
        return getNextWaypoint(waypoints);
    }

    private P getPreviousWaypoint() {
        return getNextWaypoint(waypointsReversed);
    }

    private P getNextWaypoint(P[] waypoints) {
        int reachedIndex = -1;

        int nearestWaypointIndex = -1;
        P nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < waypoints.length; i++) {
            P w = waypoints[i];
            double curDistance = w.distance(C.self);
            if (curDistance <= WAYPOINT_RADIUS && reachedIndex < i) {
                reachedIndex = i;
            }
            if (nearest == null || (nearestDistance > curDistance)) {
                nearest = w;
                nearestDistance = curDistance;
                nearestWaypointIndex = i;
            }
        }

        if (reachedIndex != -1) {
            if (reachedIndex == waypoints.length - 1) {
                return waypoints[reachedIndex];
            } else {
                return waypoints[reachedIndex + 1];
            }
        }

        if (nearestWaypointIndex == 0) {
            P p1 = waypoints[0];
            P p2 = waypoints[1];
            P self = P.from(C.self);
            if (P.perpendicularOnLine(p1, p2, self))
                return p2;
            else
                return p1;
        } else if (nearestWaypointIndex == waypoints.length - 1) {
            return waypoints[nearestWaypointIndex];
        } else {
            P p1 = waypoints[nearestWaypointIndex - 1];
            P p2 = waypoints[nearestWaypointIndex];
            P p3 = waypoints[nearestWaypointIndex + 1];
            if (p1.distanceSqr(C.self) < p3.distanceSqr(C.self)) {
                return p2;
            } else {
                return p3;
            }
        }
    }

    private P[] getWaypointsReversed() {
        P[] result = new P[waypoints.length];
        for (int i = 0; i < waypoints.length; i++) {
            result[i] = waypoints[waypoints.length - 1 - i];
        }
        return result;
    }

    private final double WAYPOINT_RADIUS = 100.0D;
}
