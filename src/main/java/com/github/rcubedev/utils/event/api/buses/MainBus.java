package com.github.rcubedev.utils.event.api.buses;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventBusBuilder;
import com.github.rcubedev.utils.event.api.spi.IEventBus;

/**
 * The main event bus. Accepts any {@link Event} subtype.
 * <p>
 * Post events via {@link #get()}:
 * <pre>
 * {@code
 * MainBus.get().register(new MyListener());
 * MainBus.get().post(new PlayerLoginEvent());
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

    private MainBus() {}

    /**
     * Singleton instance of the main event bus.
     */
    public static IEventBus<Event> get() {
        return Holder.BUS;
    }

    private static class Holder {
        private static final IEventBus<Event> BUS = EventBusBuilder.create(Event.class);
        private Holder() {}
    }

//    static {
//        BUS = new EventBus<>(Event.class, 128);
//        EventBusRegistry.getInstance().register(BUS);
//    }
}