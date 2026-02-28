package com.github.rcubedev.example.event.api;

public interface Cancellable {
    boolean isCancelled();
    void cancel();
}
