package com.github.rcubedev.utils.event.impl.apt.scanner;

import javax.lang.model.element.TypeElement;
import java.util.List;

public record DiscoveredListener(TypeElement listenerClass, List<DiscoveredMethod> handlerMethods) {}
