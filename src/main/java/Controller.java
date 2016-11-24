import model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class Controller {
    private final int NEAREST_RADIUS = 180;
    public LaneType lane = null;
    public Map<LaneType, WayPoint[]> waypointsByLane = null;
    public WayPoint[] waypoints;
    public WayPoint[] waypointsReversed;
    private int nextPointBranchCooldown = 0;
                                            // top, middle, bottom
    private final P[] firstLineTowers = new P[]{new P(1688, 50), new P(2071, 1600), new P(3650, 2344)};
    private final P[] secondLineTowers = new P[]{new P(2630, 350), new P(3098, 1232), new P(3950, 1307)};
    private final P mainTower = new P(3600, 400);

    public boolean move() {
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

        return move || attack;
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
                if (!C.targets.enemies.isEmpty() && !canAttack(C.targets.enemies.get(0))) {
                    move = C.pathfinder.goTo(C.targets.enemies.get(0));
                    if (move) Utils.debugMoveTarget("attack_enemy", C.targets.enemies.get(0), null);
                } else {
                    move = C.pathfinder.goTo(nextWaypoint, NEAREST_RADIUS);
                    if (move) Utils.debugMoveTarget("next_waypoint", null, nextWaypoint);
                }
            } else if (C.health() > 0.4) {
                // по возможности уклоняться
                nextPointBranchCooldown--;
                if (C.targets.dangers.isEmpty() && nextPointBranchCooldown <= 0) {
                    move = C.pathfinder.goTo(nextWaypoint, NEAREST_RADIUS);
                    if (move) Utils.debugMoveTarget("next_waypoint", null, nextWaypoint);
                } else if (C.targets.enemies.isEmpty()) {
                    // TODO: что делать, если я умеренно побитый, но врагов поблизости нету
                    move = true;
                } else {
                    nextPointBranchCooldown = 100;
                    move = gotoPreviousForSafety(previousWaypoint);
                }
            } else {
                // уклоняться и лечиться
                if (!C.targets.enemies.isEmpty()) {
                    move = gotoPreviousForSafety(previousWaypoint);
                } else {
                    move = true;
                    // TODO Что делать, если я побитый, но врагов поблизости нету?
                }
            }
        }

        if (!move) {
            // паффайндинг не паффайндится :(
            C.pathfinderFailureTicks++;
            Utils.debugMoveTarget("PATHFINDING_FAILURE", null, null);
        }

        return move;
    }

    private boolean gotoPreviousForSafety(P previousWaypoint) {
        boolean move = C.pathfinder.goTo(getPreviousWaypoint(), NEAREST_RADIUS);
        if (move) {
            Utils.debugMoveTarget("previous_waypoint", null, previousWaypoint);
        } else {
            P prevPrevWaypoint = null;
            for (int i = 0; i < waypoints.length; i++) {
                if (waypoints[i] == previousWaypoint) {
                    if (i > 0)
                        prevPrevWaypoint = waypoints[i - 1];
                    break;
                }
            }

            if (prevPrevWaypoint != null) {
                move = C.pathfinder.goTo(prevPrevWaypoint, NEAREST_RADIUS);
                if (move)
                    Utils.debugMoveTarget("prev_previous_waypoint", null, prevPrevWaypoint);
            }
        }

        return move;
    }

    private boolean canAttack(LivingUnit target) {
        return C.self.getDistanceTo(target) < C.self.getCastRange();
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

    // return <visible tower index; destroyed tower index>
    private Pair<Integer, Integer> nearestLaneTowerIndex() {
        int visible = 0;
        int destroyed = 0;
        P firstTower = firstLineTowers[lane.ordinal()];
        P secondTower = secondLineTowers[lane.ordinal()];

        for (Building b : C.targets.towers)
            if (firstTower.distanceSqr(b) <= 100*100) {
                visible = 1;
                break;
            }

        if (visible != 1 && firstTower.distanceSqr(C.self) < C.game.getWizardVisionRange() * C.game.getWizardVisionRange())
            destroyed = 1;

        for (Building b : C.targets.towers)
            if (secondTower.distanceSqr(b) <= 100*100) {
                visible = 2;
                break;
            }

        if (visible != 2 && secondTower.distanceSqr(C.self) < C.game.getWizardVisionRange() * C.game.getWizardVisionRange())
            destroyed = 2;

        return new Pair<>(visible, destroyed);
    }

    public void selectLane() {
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
    
    public void createWaypoints() {
        double mapSize = C.game.getMapSize();
        waypointsByLane =  new EnumMap<>(LaneType.class);
        waypointsByLane.put(LaneType.MIDDLE, new WayPoint[]{
                new WayPoint(1072, 2925, 0),
                new WayPoint(1785, 2244, 0),
                new WayPoint(2000, 2000, 0),// attack line
                new WayPoint(2214, 1755, 1),// <- after I tower destruction
                new WayPoint(2483, 1497, 1),
                new WayPoint(2678, 1313, 1),// attack line
                new WayPoint(2927, 1074, 2),// <- after II tower destruction
                new WayPoint(3353, 639, 2)
        });

        waypointsByLane.put(LaneType.TOP, new WayPoint[]{
                new WayPoint(208, 2693, 0),
                new WayPoint(175, 1656, 0),
                new WayPoint(183, 556, 0),
                new WayPoint(556, 183, 0),
                new WayPoint(1028, 183, 0),
                new WayPoint(1300, 183, 0), // attack line
                new WayPoint(1688, 208, 1), // <- after I tower destruction
                new WayPoint(1968, 183, 1),
                new WayPoint(2253, 183, 1), // attack line
                new WayPoint(2630, 175, 2), // <- after II tower destruction
                new WayPoint(3264, 183, 2)
        });

        waypointsByLane.put(LaneType.BOTTOM, new WayPoint[]{
                new WayPoint(1370, 3825, 0),
                new WayPoint(2312, 3791, 0),
                new WayPoint(3443, 3816, 0),
                new WayPoint(3816, 3443, 0),
                new WayPoint(3816, 3017, 0),
                new WayPoint(3816, 2738, 0), // attack line
                new WayPoint(3825, 2344, 1),// <- after I tower destruction
                new WayPoint(3816, 1985, 1),
                new WayPoint(3816, 1694, 1), // attack line
                new WayPoint(3791, 1307, 2),// <- after II tower destruction
                new WayPoint(3816, 735, 2)
        });
    }

    public P getNextWaypoint() {
        Pair<Integer, Integer> towersState = nearestLaneTowerIndex();
        int towerIndex = 0;
        if (towersState.a != 0 || towersState.b == 1)
            towerIndex = 1;
        if (towerIndex == 1) {
            if (towersState.a != 2 || towersState.b == 2)
                towerIndex = 2;
        }

        int maxIndex = 1000;
        int i = 0;
        for (WayPoint p : waypoints)
            if (p.towerIndex > towerIndex) {
                maxIndex = i;
                break;
            } else {
                i++;
            }
        return getNextWaypoint(waypoints, maxIndex);
    }

    public P getPreviousWaypoint() {
        return getNextWaypoint(waypointsReversed);
    }

    private P getNextWaypoint(P[] waypoints) {
        return getNextWaypoint(waypoints, 1000);
    }

    private P getNextWaypoint(P[] waypoints, int maxIndex) {
        int reachedIndex = -1;

        int nearestWaypointIndex = -1;
        P nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < waypoints.length; i++) {
            if (i > maxIndex)
                break;
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
                if (nearestWaypointIndex == maxIndex) {
                    return p2;
                } else {
                    return p3;
                }
            }
        }
    }

    private WayPoint[] getWaypointsReversed() {
        WayPoint[] result = new WayPoint[waypoints.length];
        for (int i = 0; i < waypoints.length; i++) {
            result[i] = waypoints[waypoints.length - 1 - i];
        }
        return result;
    }

    private final double WAYPOINT_RADIUS = 300.0D;

    private static class WayPoint extends P {
        public final int towerIndex;

        public WayPoint(double x, double y, int towerIndex) {
            super(x, y);
            this.towerIndex = towerIndex;
        }
    }
}
