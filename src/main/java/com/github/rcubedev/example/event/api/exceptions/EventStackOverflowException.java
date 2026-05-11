package com.github.rcubedev.example.event.api.exceptions;

/**
 * Thrown when the {@link com.github.rcubedev.example.event.api.spi.IEventBus IEventBus}
 * detects a recursion depth exceeding its configured safety limit.
 * <p>
 * This usually indicates a circular event post (e.g., Event A posts Event B, 
 * which in turn posts Event A).
 */
public class EventStackOverflowException extends RuntimeException {

    private final int depth;
    private final int maxDepth;

    public EventStackOverflowException(String message, int depth, int maxDepth) {
        super(message);
        this.depth = depth;
        this.maxDepth = maxDepth;
    }

    /**
     * @return The depth attempted that triggered the guard.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return The maximum allowed depth for the bus.
     */
    public int getMaxDepth() {
        return maxDepth;
    }
}