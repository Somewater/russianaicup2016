import model.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public final class MyStrategy implements Strategy {
    MyStrategyFromDocs strategyFromDocs = new MyStrategyFromDocs();

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initNewTick(self, world, game, move);

        //strategyFromDocs.move(self, world, game, move);

        trackTarget();

        completeTick();
    }

    private void initNewTick(Wizard self, World world, Game game, Move move) {
        C.debug = System.getenv("DEBUG") != null && System.getenv("DEBUG").startsWith("t");
        C.startTickTime = System.currentTimeMillis();
        C.self = self;
        C.world = world;
        C.game = game;
        C.move = move;
        if (C.pathfinder == null)
            C.pathfinder = new Pathfinder2();
        if (C.vis == null)
            C.vis = new VisualClient(!C.debug);

        C.pathfinder.initNewTick();
        if (C.debug) {
            C.vis.beginPost();
        }
        C.maxSpeed = C.game.getWizardForwardSpeed() + C.game.getWizardStrafeSpeed();
        C.targets = new NearestTargets().init();
    }

    private void completeTick() {
        if (C.debug) {
            C.pathfinder.debugDrawMap();
            C.vis.endPost();
        }
        long duration = System.currentTimeMillis() - C.startTickTime;
        System.out.print("Tick duration: "); System.out.print(duration); System.out.print(" ms\n");
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
        ws.sort(new Comparator<Wizard>() {
            @Override
            public int compare(Wizard o1, Wizard o2) {
                double diff = P.from(o1).distance(P.from(C.self)) - P.from(o2).distance(P.from(C.self));
                return (int)diff;
            }
        });
        target = ws.get(ws.size() - 1);

        if (target != null) {
            P[] path = C.pathfinder.path(P.from(C.self), P.from(target));
            P prev = null;

            for (P p : path) {
                if (prev != null) {
                    C.vis.line(prev.x, prev.y, p.x, p.y, Color.RED);
                }
                prev = p;
            }
            C.pathfinder.goTo(path);
        }
    }
}