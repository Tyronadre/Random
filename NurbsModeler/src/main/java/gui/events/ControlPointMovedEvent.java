package gui.events;

import data.ControlPoint;

import java.util.EventObject;
import java.util.List;

public class ControlPointMovedEvent extends EventObject {
    public ControlPointMovedEvent(ControlPoint controlPoint, double[] oldValues, double[] newValues) {
        super(controlPoint);
    }
}
