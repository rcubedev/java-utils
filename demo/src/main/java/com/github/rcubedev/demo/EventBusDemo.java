package com.github.rcubedev.demo;

import com.github.rcubedev.utils.event.api.*;
import com.github.rcubedev.utils.event.api.annotation.CompiledEventHandlers;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.buses.MainBus;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadLocalRandom;

@CompiledEventHandlers
public final class EventBusDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusDemo.class);
    private static int h1EncNum = 0;

    public static void run() {
        LOGGER.info("Starting EventBus Demo");
        LOGGER.info("Registering handler");
        Subscription sub = MainBus.get().register(EventBusDemo.class, Identity.of(MethodHandles.lookup()));
        LOGGER.info("Manually registering handler #5 via callback at Priority.MONITOR");
        Subscription callbackSub = MainBus.get().register(Event.class, Priority.MONITOR, e -> {
            LOGGER.info("Handler #5 (Callback) Recieved Event or subtype: {}", e);
        }, Identity.ofPublic());
        LOGGER.info("");

        CustomEvent event = new CustomEvent(ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE));
        LOGGER.info("Created event: {}", event);
        LOGGER.info("Dispatching... This will fire handlers #2, 3, 4, 5 in that order.");
        event.dispatch();
        LOGGER.info("");

        event = new CancellableCustomEvent(ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE));
        LOGGER.info("Created event: {}", event);
        LOGGER.info("Dispatching... This will fire handlers #1, 2, 3 and 4, 5 in that order.");
        event.dispatch();
        LOGGER.info("");

        event = new CancellableCustomEvent(ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE));
        LOGGER.info("Created event: {}", event);
        LOGGER.info("Cancelling subscription for handler #5; the event will never be received on it again.");
        callbackSub.unsubscribe();
        LOGGER.info("Dispatching... This will fire handlers #1, 2, 4 in that order as #1 will cancel the event so that #3 will not receive it & #5 has been cancelled");
        event.dispatch();
        LOGGER.info("");

        LOGGER.info("Cancelling subscription for handlers 1-4; the event will no longer be received on them.");
        sub.unsubscribe();
        LOGGER.info("Dispatching...");
        event.dispatch();
        LOGGER.info("No handlers should have fired.\n");
    }

    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onCancellableCustomEvent(CancellableCustomEvent event) {
        LOGGER.info("Handler #1 (Priority.LOWEST, before LOW) Received CancellableCustomEvent or subtype. #toString: {}", event);
        if (++h1EncNum == 2) {
            LOGGER.info("Handler #1 Cancelling received event. The event will not be received by Handler #3 as it ignores cancelled events.");
            event.cancel();
            return;
        }
        LOGGER.info("Handler #1 Not cancelling received event as this handler will only cancel the 2nd witnessed event and has witnessed {} events", h1EncNum);
    }

    @SubscribeEvent(priority = Priority.LOW)
    public static void onCustomEvent(CustomEvent event) {
        LOGGER.info("Handler #2 (Priority.LOW, after LOWEST) Received CustomEvent or subtype. #toString: {}", event);
    }

    @SubscribeEvent(priority = Priority.NORMAL, ignoreCancelled = true)
    public static void onCustomEventIgnore(Event event) {
        LOGGER.info("Handler #3 (Priority.NORMAL) Received Event or subtype (this event was not cancelled). #toString: {}", event);
    }

    @SubscribeEvent(priority = Priority.HIGH)
    public static void onAnyEventMonitor(Event event) {
        LOGGER.info("Handler #4 (Priority.HIGH, after NORMAL) Received Event or subtype. #toString: {}", event);
    }

    public static class CustomEvent extends Event {
        private final int data;

        public CustomEvent(int data) {
            this.data = data;
        }

        public int getData() {
            return this.data;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[data=" + data + "]";
        }
    }

    public static class CancellableCustomEvent extends CustomEvent implements Cancellable {
        private volatile boolean cancelled = false;

        public CancellableCustomEvent(int data) {
            super(data);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[data=" + getData() + ", cancelled=" + cancelled + "]";
        }
    }
}