import model.Unit;

public class NaivePathfinder implements IPathfinder {
    @Override
    public void initNewTick() {

    }

    @Override
    public P[] path(P src, P dest) {
        return new P[]{src, dest};
    }

    @Override
    public boolean goTo(P[] path) {
        if (path != null && path.length > 0)
            Utils.goTo(path[path.length - 1], false);
        return true;
    }

    @Override
    public boolean goTo(P target) {
        Utils.goTo(target, false);
        return true;
    }

    @Override
    public boolean goTo(P target, double radius) {
        Utils.goTo(target, false);
        return true;
    }

    @Override
    public boolean goTo(Unit target) {
        Utils.goTo(P.from(target), false);
        return true;
    }
}
