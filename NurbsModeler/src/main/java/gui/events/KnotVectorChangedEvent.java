package gui.events;

import java.util.EventObject;

public class KnotVectorChangedEvent extends EventObject {
    private final double[] newKnotVector;

    public KnotVectorChangedEvent(Object source, double[] knotVector) {
        super(source);
        this.newKnotVector = knotVector;
    }

    public double[] getNewKnotVector() {
        return newKnotVector;
    }
}
