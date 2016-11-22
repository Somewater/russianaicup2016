import model.Unit;

public interface IPathfinder {
    void initNewTick();

    P[] path(P src, P dest);

    boolean goTo(P[] path);
    boolean goTo(P target);
    boolean goTo(P target, double radius);
    boolean goTo(Unit target);
}
