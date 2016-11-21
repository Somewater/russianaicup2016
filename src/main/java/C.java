import model.Game;
import model.Move;
import model.Wizard;
import model.World;

/**
 * Состояние мира в момент тика, C - context
 */
public class C {
    public static boolean debug = false;
    public static Wizard self;
    public static World world;
    public static Game game;
    public static Move move;

    public static Pathfinder2 pathfinder;
    public static VisualClient vis;
    public static NearestTargets targets;

    public static double maxSpeed;
    public static long startTickTime = 0;
    public static long tickDurationSum = 0;

    public static double health() {
        return ((double)self.getLife()) / self.getMaxLife();
    }

    public static double mana() {
        return ((double)self.getMana()) / self.getMaxMana();
    }
}
