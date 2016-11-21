import com.sun.corba.se.impl.javax.rmi.CORBA.Util;
import model.*;

import java.util.ArrayList;

public class NearestTargets {
    public ArrayList<LivingUnit> allEnemies;

    // враги в поле действия атаки
    public ArrayList<LivingUnit> enemies;

    // бонусы в разумной удалённости
    public ArrayList<Bonus> bonuses;

    // юниты, которые могут атаковать
    public ArrayList<Pair<Double, LivingUnit>> dangers;

    public NearestTargets init() {
        allEnemies = new ArrayList<>();
        enemies = new ArrayList<>();
        bonuses = new ArrayList<>();
        dangers = new ArrayList<>();
        findEnemies();
        findBonuses();
        findDangers();

        return this;
    }

    private void findEnemies() {
        double attackRangeSqr = C.self.getCastRange() * C.self.getCastRange();
        for (LivingUnit u : C.world.getBuildings())
            if (u.getFaction() != Faction.NEUTRAL && u.getFaction() != C.self.getFaction()) {
                if (Utils.distanceSqr(u) <= attackRangeSqr)
                    enemies.add(u);
                allEnemies.add(u);
            }
        for (LivingUnit u : C.world.getWizards())
            if (u.getFaction() != Faction.NEUTRAL && u.getFaction() != C.self.getFaction()) {
                if (Utils.distanceSqr(u) <= attackRangeSqr)
                    enemies.add(u);
                allEnemies.add(u);
            }
        for (LivingUnit u : C.world.getMinions())
            if (u.getFaction() != Faction.NEUTRAL && u.getFaction() != C.self.getFaction()) {
                if (Utils.distanceSqr(u) <= attackRangeSqr)
                    enemies.add(u);
                allEnemies.add(u);
            }
        allEnemies.sort(Utils.distanceCmp);
        enemies.sort(Utils.distanceCmp);
    }

    private void findBonuses() {
        int maxTicks = 100;
        for (Bonus b : C.world.getBonuses()) {
            int maxTicksCur = maxTicks;
            if (b.getType() == BonusType.SHIELD)
                maxTicksCur = (int)(maxTicksCur + (1.0 - C.health()) * 300);
            if (C.pathfinder.distance(b) < maxTicksCur * C.maxSpeed)
                bonuses.add(b);
        }
        bonuses.sort(Utils.distanceCmp);
    }

    private void findDangers() {
        for (LivingUnit e : allEnemies) {
            double distanceSqr = Utils.distanceSqr(e);
            if (e instanceof Building) {
                Building b = (Building) e;
                if (b.getAttackRange() * b.getAttackRange() >= distanceSqr) {
                    double damage = ((double) b.getDamage()) / C.self.getMaxLife();
                    dangers.add(new Pair(damage, b));
                }
            } else if (e instanceof Minion) {
                Minion m = (Minion) e;
                double attackRange = 0;
                if (m.getType() == MinionType.FETISH_BLOWDART) {
                    attackRange = C.game.getFetishBlowdartAttackRange();
                } else if (m.getType() == MinionType.ORC_WOODCUTTER) {
                    attackRange = C.game.getOrcWoodcutterAttackRange();
                } else
                    throw new RuntimeException("Unknown minion type " + m.getType());
                if (attackRange * attackRange >= distanceSqr) {
                    double damage = ((double) m.getDamage()) / C.self.getMaxLife();
                    dangers.add(new Pair(damage, m));
                }
            } else if (e instanceof Wizard) {
                Wizard w = (Wizard) e;
                if (w.getCastRange() * w.getCastRange() >= distanceSqr) {
                    double damage = ((double) C.game.getMagicMissileDirectDamage()) / C.self.getMaxLife();
                    dangers.add(new Pair(damage, w));
                }
            } else {
                throw new RuntimeException("Undefined enemy " + e.toString());
            }
        }
    }
}
