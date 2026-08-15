package com.github.rcubedev.demo;

import com.github.rcubedev.utils.registry.api.mutable.mapped.MutableMappedIdRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegistryDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryDemo.class);

    public static void run() {
        LOGGER.info("Starting Registry Demo");

        MutableMappedIdRegistry<String, String> registry = MutableMappedIdRegistry.create("config-registry");

        int hostId = registry.registerId("server.host", "127.0.0.1");
        int portId = registry.registerId("server.port", "8080");

        LOGGER.info("Registered 'server.host' -> assigned ID: {}", hostId);
        LOGGER.info("Registered 'server.port' -> assigned ID: {}", portId);
        LOGGER.info("");

        LOGGER.info("Freezing registry...");
        registry.freeze();
        LOGGER.info("");

        String host = registry.getById(hostId).orElse("unknown");
        LOGGER.info("Post freeze lookup (server.host) by ID ({}): {}", hostId, host);

        String key = "server.port";
        String port = registry.get(key).orElse("unknown");
        LOGGER.info("Post freeze lookup (server.port) by key ({}): {}\n", key, port);
    }
}