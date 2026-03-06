package com.github.rcubedev.example.event.buses;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventBus;
import com.github.rcubedev.example.event.api.IEventBus;

/**
 * The main event bus. Accepts any {@link Event} subtype.
 * <p>
 * Post events via {@link #BUS}:
 * <pre>
 * {@code
 * MainBus.BUS.post(new PlayerLoginEvent());
 * MainBus.BUS.register(new MyListener());
 * }
 * </pre>
 * For a scoped bus that only accepts specific event subtypes, extend {@link EventBus} directly:
 * <pre>
 * {@code
 * public abstract class MyModEvent extends Event {}
 *
 * public final class MyModBus extends EventBus<MyModEvent> {
 *     public static final MyModBus INSTANCE = new MyModBus();
 *     private MyModBus() { super(MyModEvent.class); }
 * }
 * }
 * </pre>
 */
public final class MainBus extends EventBus<Event> {

    /** Singleton instance of the main event bus. */
    public static final MainBus INSTANCE = new MainBus();

    /** Convenience alias for {@link #INSTANCE}. */
    public static final IEventBus<Event> BUS = INSTANCE;

    private MainBus() {
        super(Event.class);
    }
}