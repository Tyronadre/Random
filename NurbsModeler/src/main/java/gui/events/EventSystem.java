package gui.events;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;

public class EventSystem {
    private static EventSystem instance;
    private final HashMap<Class<?>, List<EventListener>> listeners = new HashMap<>();

    private EventSystem() {
    }

    public static EventSystem getInstance() {
        if (instance == null) {
            instance = new EventSystem();
        }
        return instance;
    }

    public <T extends EventObject> void  subscribe(Class<T> clazz, DefaultEventListener<T> listener) {
        this.listeners.putIfAbsent(clazz, new ArrayList<>());
        this.listeners.get(clazz).add(listener);
    }

    public <T extends EventObject> void dispatch(T eventObject) {
        if (this.listeners.containsKey(eventObject.getClass())) {
            for (EventListener listener : this.listeners.get(eventObject.getClass())) {
                try {
                    ((DefaultEventListener<T>) listener).handle(eventObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
