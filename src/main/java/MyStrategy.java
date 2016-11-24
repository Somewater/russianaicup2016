import model.*;

import java.util.ArrayList;
import java.util.Random;

public final class MyStrategy implements Strategy {
    MyStrategyFromDocs strategyFromDocs = new MyStrategyFromDocs();
    Controller controller = new Controller();

    Pathfinder2 pf2;
    Pathfinder pf;
    NaivePathfinder npf;
    private int pfSwapTickCooldown = 0;
    private final int PF_SWAP_COOLDOWN = 200;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initNewTick(self, world, game, move);

        //trackTarget();
        if (!controller.move())
            strategyFromDocs.move(self, world, game, move);

        completeTick();
    }

    private void initNewTick(Wizard self, World world, Game game, Move move) {
        C.startTickTime = System.currentTimeMillis();
        C.self = self;
        C.world = world;
        C.game = game;
        C.move = move;
        if (C.random == null)
            C.random = new Random(game.getRandomSeed());
        if (C.pathfinder == null)
            C.pathfinder = pf2 = new Pathfinder2();

        if (C.pathfinderFailureTicks >= PF_SWAP_COOLDOWN) {
            if (pfSwapTickCooldown-- <= 0) {
                pfSwapTickCooldown = PF_SWAP_COOLDOWN;
                swapPathFinder();
            }
        } else {
            pfSwapTickCooldown = 0;
        }

        if (C.vis == null && C.debug)
            C.vis = new VisualClient(!C.debug);

        C.pathfinder.initNewTick();
        if (C.debug) {
            C.vis.beginPost();
        }
        C.maxSpeed = C.game.getWizardForwardSpeed() + C.game.getWizardStrafeSpeed();
        C.targets = new NearestTargets().init();
    }

    private void swapPathFinder() {
        if (C.pathfinder instanceof Pathfinder2) {
            if (pf == null) pf = new Pathfinder();
            C.pathfinder = pf;
        } else if (C.pathfinder instanceof Pathfinder) {
            if(npf == null) npf = new NaivePathfinder();
            C.pathfinder = npf;
        } else {
            C.pathfinder = pf2;
        }
    }

    private void completeTick() {
        if (C.debug) {
            if (C.pathfinder instanceof Pathfinder2) ((Pathfinder2) C.pathfinder).debugDrawMap();
            C.vis.endPost();
        }
        long duration = System.currentTimeMillis() - C.startTickTime;
        if (C.debug) {
            System.out.print("[TICK\t" + String.valueOf(C.world.getTickIndex()) + ",\t");
            System.out.print(duration);
            System.out.print(" ms]\t");
            Utils.printDebugMoveTarget();
            System.out.print("\n");
        }
        C.tickDurationSum += duration;
        C.targets = null;
    }

    private int f = 4;

    private void trackTarget() {
        if (C.world.getTickIndex() == 350)
            f = 2;
        if (C.world.getTickIndex() < 10)
            return;
        Unit target = null;
//        for (Wizard w : world.getWizards())
//            if (w.getFaction() != self.getFaction()) {
//                target = w;
//                break;
//            }
        ArrayList<Wizard> ws = new ArrayList<>();
        if (target == null) {
            for (Wizard w : C.world.getWizards())
                if (!w.isMe() && w.getOwnerPlayerId() == f) {
                    ws.add(w);
                }
        }
        ws.sort(Utils.distanceCmp);
        if (!ws.isEmpty())
            target = ws.get(ws.size() - 1);

        if (target != null) {
            P[] path = C.pathfinder.path(P.from(C.self), P.from(target));
            Utils.drawPath(path);
            C.pathfinder.goTo(path);
        }
    }
}