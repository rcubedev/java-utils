package com.github.rcubedev.demo;

import com.github.rcubedev.utils.services.api.ServiceBootstrap;
import com.github.rcubedev.utils.services.api.ServiceRegistry;
import com.github.rcubedev.utils.services.api.spi.Service;
import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public final class ServiceDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDemo.class);

    private ServiceDemo() {}

    public static void run() {
        LOGGER.info("Starting Service API Demo");
        ServiceLayer layer = ServiceBootstrap.classLoader("CoreServices",
                ServiceDemo.class.getClassLoader(), 100);

        LOGGER.info("Bootstrapped service layer: '{}' (priority {})", layer.name(), layer.priority());
        ServiceRegistry registry = ServiceBootstrap.bootstrap(layer);

        LOGGER.info("Locating service provider for class {}", MyService.class.getName());
        Service<MyService> exampleServiceL = registry.find(MyService.class).orElseThrow();
        MyService exampleService = exampleServiceL.get();
        LOGGER.info("Located service {} for requested class {}\n", exampleService, MyService.class.getName());
    }

    public interface MyService {
        int getValue();
    }

    public static final class MyServiceImpl implements MyService {
        @Override
        public int getValue() {
            return ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE);
        }
    }
}
