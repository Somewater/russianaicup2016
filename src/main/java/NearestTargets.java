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

    public Building nearestTower;
    public ArrayList<Building> towers;
    public Building mainTower;
    public LivingUnit bestVictim = null;

    public NearestTargets init() {
        allEnemies = new ArrayList<>();
        enemies = new ArrayList<>();
        bonuses = new ArrayList<>();
        dangers = new ArrayList<>();
        towers = new ArrayList<>();
        findEnemies();
        findBonuses();
        findDangers();
        bestVictim = findBestVictim();

        return this;
    }

    private void findEnemies() {
        double attackRangeSqr = C.self.getCastRange() * C.self.getCastRange();
        for (Building u : C.world.getBuildings())
            if (u.getFaction() != Faction.NEUTRAL && u.getFaction() != C.self.getFaction()) {
                if (Utils.distanceSqr(u) <= attackRangeSqr)
                    enemies.add(u);
                if (Utils.distanceSqr(u) < 360000)
                    allEnemies.add(u);

                if (u.getType() == BuildingType.FACTION_BASE)
                    mainTower = u;
                else
                    towers.add(u);

                if (nearestTower == null || nearestTower.getDistanceTo(C.self) > u.getDistanceTo(C.self)) {
                    nearestTower = u;
                }
            }
        for (LivingUnit u : C.world.getWizards())
            if (u.getFaction() != Faction.NEUTRAL && u.getFaction() != C.self.getFaction()) {
                if (Utils.distanceSqr(u) <= attackRangeSqr)
                    enemies.add(u);
                if (Utils.distanceSqr(u) < 360000)
                    allEnemies.add(u);
            }
        for (LivingUnit u : C.world.getMinions())
            if (u.getFaction() != Faction.NEUTRAL && u.getFaction() != C.self.getFaction()) {
                if (Utils.distanceSqr(u) <= attackRangeSqr)
                    enemies.add(u);
                if (Utils.distanceSqr(u) < 360000)
                    allEnemies.add(u);
            }
        allEnemies.sort(Utils.distanceCmp);
        enemies.sort(Utils.distanceCmp);
        towers.sort(Utils.distanceCmp);
    }

    private void findBonuses() {
        int maxTicks = 100;
        for (Bonus b : C.world.getBonuses()) {
            int maxTicksCur = maxTicks;
            if (b.getType() == BonusType.SHIELD)
                maxTicksCur = (int)(maxTicksCur + (1.0 - C.health()) * 300);
            double dist = maxTicksCur * C.maxSpeed;
            if (Utils.distanceSqr(b) < dist * dist)
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

    private LivingUnit findBestVictim() {
        LivingUnit victim = null;
        boolean victimCanAttack = false;
        double victimScore = -1;

        for (LivingUnit e : allEnemies) {
            double distance = StrictMath.sqrt(Utils.distanceSqr(e));
            boolean canAttack = distance < C.self.getCastRange();
            double score = getScores(e);
            if (distance > 500)
                score = score * 0.8;
            else if (distance > 300)
                score = score * 0.9;

            if (victim == null) {
                victim = e;
                victimCanAttack = canAttack;
                victimScore = score;
            } else {
                if (!victimCanAttack) {
                    if  (canAttack || score > victimScore) {
                        victim = e;
                        victimCanAttack = canAttack;
                        victimScore = score;
                    }
                } else if (canAttack && score > victimScore) {
                    victim = e;
                    victimCanAttack = canAttack;
                    victimScore = score;
                }
            }
        }

        return victim;
    }

    private int getScores(LivingUnit u){
        int life = u.getLife();
        int killBonus = (int)(u.getMaxLife() * 0.01 * (1.0 - ((double)u.getLife()) / u.getMaxLife()));
        if (u.getLife() <= C.game.getMagicMissileDirectDamage()) {
            if (u instanceof Wizard)
                killBonus = u.getMaxLife();
            else
                killBonus = (int)(u.getMaxLife() * 0.25);
        }

        if (u instanceof Building || u instanceof Wizard)
            return ((int)(0.25 * life)) + killBonus;
        else
            return ((int)(0.01 * life))+ killBonus;
    }
}
