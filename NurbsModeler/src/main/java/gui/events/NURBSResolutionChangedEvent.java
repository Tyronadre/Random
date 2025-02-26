package gui.events;

import java.util.EventObject;

public class NURBSResolutionChangedEvent extends EventObject {
    private final int newResolution;

    public NURBSResolutionChangedEvent(Object source, int resolution) {
        super(source);
        this.newResolution = resolution;
    }

    public int getNewResolution() {
        return newResolution;
    }
}
