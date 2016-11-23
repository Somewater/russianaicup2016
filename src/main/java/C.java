import model.*;

import java.util.Random;

/**
 * Состояние мира в момент тика, C - context
 */
public class C {
    public static boolean debug = false;
    public static Wizard self;
    public static World world;
    public static Game game;
    public static Move move;
    public static Random random;

    public static IPathfinder pathfinder;
    public static VisualClient vis;
    public static NearestTargets targets;

    public static double maxSpeed;
    public static long startTickTime = 0;
    public static long tickDurationSum = 0;
    public static int tickDurationMs() {
        return (int)(System.currentTimeMillis() - startTickTime);
    }

    public static double health() {
        return ((double)self.getLife()) / self.getMaxLife();
    }

    public static double mana() {
        return ((double)self.getMana()) / self.getMaxMana();
    }

    public static int pathfinderFailureTicks = 0;
    public static int pathFromCache = 0;

    public static Triple<String, Unit, P> moveTarget = null;
}
