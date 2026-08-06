package com.github.rcubedev.utils.event.api;

public interface Cancellable {
    boolean isCancelled();
    void cancel();
}
