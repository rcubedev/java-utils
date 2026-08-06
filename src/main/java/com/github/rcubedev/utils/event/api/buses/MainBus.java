package com.github.rcubedev.utils.event.api.buses;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventBusBuilder;
import com.github.rcubedev.utils.event.api.spi.IEventBus;

/**
 * The main event bus. Accepts any {@link Event} subtype.
 * <p>
 * Post events via {@link #BUS}:
 * <pre>
 * {@code
 * MainBus.BUS.register(new MyListener());
 * MainBus.BUS.post(new PlayerLoginEvent());
 * }
 * </pre>
 * For a scoped bus that only accepts specific event subtypes, create via {@link EventBusBuilder}:
 * <pre>
 * {@code
 * public abstract class MyModEvent extends Event {}
 *
 * public final class MyModBus {
 *     public static final IEventBus<MyModEvent> INSTANCE =
 *             EventBusBuilder.create(MyModEvent.class);
 * }
 * }
 * </pre>
 */
public final class MainBus {

    /**
     * Singleton instance of the main event bus.
     */
    public static final IEventBus<Event> BUS = EventBusBuilder.create(Event.class);

    private MainBus() {}

//    static {
//        BUS = new EventBus<>(Event.class, 128);
//        EventBusRegistry.getInstance().register(BUS);
//    }
}