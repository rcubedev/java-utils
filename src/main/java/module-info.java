module com.github.rcubedev.example {
    requires org.slf4j; // fixme this might be gone soon

    requires com.google.common;
    requires kaleido.config; // fixme possible own config lib in future

    requires static org.jetbrains.annotations;

    exports com.github.rcubedev.example.event.api;
    exports com.github.rcubedev.example.event.api.spi;

    exports com.github.rcubedev.example.services.api;
    exports com.github.rcubedev.example.services.api.spi;

    //fixme temp
    exports com.github.rcubedev.example.event.api.buses;
    exports com.github.rcubedev.example;
    exports com.github.rcubedev.example.config;
    exports com.github.rcubedev.example.reflect;
    exports com.github.rcubedev.example.services.impl.layer;
}