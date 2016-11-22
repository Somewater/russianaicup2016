import model.*;

import java.util.EnumMap;
import java.util.Map;

public class Controller {
    private LaneType lane = null;
    private Map<LaneType, P[]> waypointsByLane = null;
    private P[] waypoints;
    private int nextPointBranchCooldown = 0;

    public void move() {
        if (waypointsByLane == null)
            createWaypoints();
        selectLane();

        setMove();
        setAttack();
    }

    private void setMove() {
        boolean move = false;
        if (!C.targets.bonuses.isEmpty() && Utils.distanceSqr(C.targets.bonuses.get(0)) < 500 * 500)
            move = C.pathfinder.goTo(C.targets.bonuses.get(0));

        if (!move) {
            if (C.health() > 0.9) {
                // идти в бой
                if (!C.targets.enemies.isEmpty()) {
                    move = C.pathfinder.goTo(C.targets.enemies.get(0));
                } else {
                    move = C.pathfinder.goTo(getNextWaypoint(), 300);
                }
            } else if (C.health() > 0.5) {
                // по возможности уклоняться
                nextPointBranchCooldown--;
                if (C.targets.dangers.isEmpty() && nextPointBranchCooldown <= 0)
                    move = C.pathfinder.goTo(getNextWaypoint(), 300);
                else {
                    nextPointBranchCooldown = 100;
                    move = C.pathfinder.goTo(getPreviousWaypoint(), 300);
                }
            } else {
                // уклоняться и лечиться
                move = C.pathfinder.goTo(getPreviousWaypoint(), 300);
            }
        }

        if (!move) {
            // паффайндинг не паффайндится :(
            C.pathfinderFailureTicks++;
        }
    }

    private void setAttack() {
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
                    } else if (cooldowns[ActionType.MAGIC_MISSILE.ordinal()] <= 0) {
                        C.move.setAction(ActionType.MAGIC_MISSILE);
                        C.move.setCastAngle(angle);
                        C.move.setMinCastDistance(distance - victim.getRadius() + C.game.getMagicMissileRadius());
                    } else if (cooldowns[ActionType.FROST_BOLT.ordinal()] <= 0 && C.mana() > 0.5) {
                        C.move.setAction(ActionType.FROST_BOLT);
                        C.move.setCastAngle(angle);
                        C.move.setMinCastDistance(distance - victim.getRadius() + C.game.getMagicMissileRadius());
                    } else if (cooldowns[ActionType.FIREBALL.ordinal()] <= 0 && C.mana() > 0.7) {
                        C.move.setAction(ActionType.FIREBALL);
                        C.move.setCastAngle(angle);
                        C.move.setMinCastDistance(distance - victim.getRadius() + C.game.getMagicMissileRadius());
                    }
                }
            }
        }
    }

    private void selectLane() {
        Message[] messages = C.self.getMessages();
        if (messages.length > 0) {
            if (messages[0].getLane() != null) {
                lane = messages[0].getLane();
                waypoints = waypointsByLane.get(lane);
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
        }
    }
    
    private void createWaypoints() {
        double mapSize = C.game.getMapSize();
        waypointsByLane =  new EnumMap<>(LaneType.class);
        waypointsByLane.put(LaneType.MIDDLE, new P[]{
                new P(100.0D, mapSize - 100.0D),
                new P(600.0D, mapSize - 200.0D),
                new P(800.0D, mapSize - 800.0D),
                new P(mapSize - 600.0D, 600.0D)
        });

        waypointsByLane.put(LaneType.TOP, new P[]{
                new P(100.0D, mapSize - 100.0D),
                new P(100.0D, mapSize - 400.0D),
                new P(200.0D, mapSize - 800.0D),
                new P(200.0D, mapSize * 0.75D),
                new P(200.0D, mapSize * 0.5D),
                new P(200.0D, mapSize * 0.25D),
                new P(200.0D, 200.0D),
                new P(mapSize * 0.25D, 200.0D),
                new P(mapSize * 0.5D, 200.0D),
                new P(mapSize * 0.75D, 200.0D),
                new P(mapSize - 200.0D, 200.0D)
        });

        waypointsByLane.put(LaneType.BOTTOM, new P[]{
                new P(100.0D, mapSize - 100.0D),
                new P(400.0D, mapSize - 100.0D),
                new P(800.0D, mapSize - 200.0D),
                new P(mapSize * 0.25D, mapSize - 200.0D),
                new P(mapSize * 0.5D, mapSize - 200.0D),
                new P(mapSize * 0.75D, mapSize - 200.0D),
                new P(mapSize - 200.0D, mapSize - 200.0D),
                new P(mapSize - 200.0D, mapSize * 0.75D),
                new P(mapSize - 200.0D, mapSize * 0.5D),
                new P(mapSize - 200.0D, mapSize * 0.25D),
                new P(mapSize - 200.0D, 200.0D)
        });
    }

    private P getNextWaypoint() {
        int lastWaypointIndex = waypoints.length - 1;
        P lastWaypoint = waypoints[lastWaypointIndex];

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            P waypoint = waypoints[waypointIndex];

            if (waypoint.distance(C.self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.distance(waypoint) < lastWaypoint.distance(C.self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    private P getPreviousWaypoint() {
        P firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            P waypoint = waypoints[waypointIndex];

            if (waypoint.distance(C.self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.distance(waypoint) < firstWaypoint.distance(C.self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    private final double WAYPOINT_RADIUS = 100.0D;
}
