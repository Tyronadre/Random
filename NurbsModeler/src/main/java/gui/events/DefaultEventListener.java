package gui.events;

import java.util.EventListener;
import java.util.EventObject;

public interface DefaultEventListener<T extends EventObject> extends EventListener {
    void handle(T event);
}
