import model.*;

import java.util.*;

public final class MyStrategyFromDocsModified implements Strategy {
    private static final double WAYPOINT_RADIUS = 100.0D;

    private static final double LOW_HP_FACTOR = 0.25D;

    private final Controller controller;

    public MyStrategyFromDocsModified(Controller controller) {
        this.controller = controller;
    }

    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        if (controller.waypointsByLane == null)
            controller.createWaypoints();
        controller.selectLane();

        //move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());

        // Если осталось мало жизненной энергии, отступаем к предыдущей ключевой точке на линии.
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            goTo(getPreviousWaypoint());
            return;
        }

        LivingUnit nearestTarget = getNearestTarget();

        // Если видим противника ...
        if (nearestTarget != null) {
            double distance = self.getDistanceTo(nearestTarget);

            // ... и он в пределах досягаемости наших заклинаний, ...
            if (distance <= self.getCastRange()) {
                double angle = self.getAngleTo(nearestTarget);

                // ... то поворачиваемся к цели.
                move.setTurn(angle);

                // Если цель перед нами, ...
                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    // ... то атакуем.
                    move.setAction(ActionType.MAGIC_MISSILE);
                    move.setCastAngle(angle);
                    move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());
                }

                return;
            }
        }

        // Если нет других действий, просто продвигаемся вперёд.
        goTo(getNextWaypoint());
    }

    /**
     * Данный метод предполагает, что все ключевые точки на линии упорядочены по уменьшению дистанции до последней
     * ключевой точки. Перебирая их по порядку, находим первую попавшуюся точку, которая находится ближе к последней
     * точке на линии, чем волшебник. Это и будет следующей ключевой точкой.
     * <p>
     * Дополнительно проверяем, не находится ли волшебник достаточно близко к какой-либо из ключевых точек. Если это
     * так, то мы сразу возвращаем следующую ключевую точку.
     */
    private P getNextWaypoint() {
        return controller.getNextWaypoint();
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    private P getPreviousWaypoint() {
        return controller.getPreviousWaypoint();
    }

    /**
     * Простейший способ перемещения волшебника.
     */
    private void goTo(P point) {
        Utils.goTo(point, false);
    }

    /**
     * Находим ближайшую цель для атаки, независимо от её типа и других характеристик.
     */
    private LivingUnit getNearestTarget() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(C.world.getBuildings()));
        targets.addAll(Arrays.asList(C.world.getWizards()));
        targets.addAll(Arrays.asList(C.world.getMinions()));

        LivingUnit nearestTarget = null;
        double nearestTargetDistance = Double.MAX_VALUE;

        for (LivingUnit target : targets) {
            if (target.getFaction() == Faction.NEUTRAL || target.getFaction() == C.self.getFaction()) {
                continue;
            }

            double distance = C.self.getDistanceTo(target);

            if (distance < nearestTargetDistance) {
                nearestTarget = target;
                nearestTargetDistance = distance;
            }
        }

        return nearestTarget;
    }
}