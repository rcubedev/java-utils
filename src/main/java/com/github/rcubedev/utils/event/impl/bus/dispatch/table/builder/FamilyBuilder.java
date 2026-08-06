package com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrySnapshot;

import java.util.*;

public class FamilyBuilder<B extends Event> {

    private final RegistrySnapshot<B> snapshot;
    private final RegisteredParentResolver<B> resolver;

    public FamilyBuilder(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver) {
        this.snapshot = snapshot;
        this.resolver = resolver;
    }

    public List<List<Class<? extends B>>> buildFamilies() {
        List<Class<? extends B>> sortedTypes = new ArrayList<>(snapshot.getHandlers().keySet());
        sortedTypes.sort(Comparator.comparingInt(resolver::hierarchyDepth));

        // Families are just linear chains where each child's parent is the element before it.
        List<List<Class<? extends B>>> lineages = new ArrayList<>(sortedTypes.size());
        Map<Class<? extends B>, List<Class<? extends B>>> lineageCache = HashMap.newHashMap(sortedTypes.size());

        for (Class<? extends B> type : sortedTypes) {
            Class<? extends B> parent = resolver.getRegisteredParentAsExtendsBus(type);
            List<Class<? extends B>> lineage;

            if (parent != null) {
                List<Class<? extends B>> parentLineage = lineageCache.get(parent);
                lineage = new ArrayList<>(parentLineage.size() + 1);
                lineage.addAll(parentLineage);
            } else lineage = new ArrayList<>(4);

            lineage.add(type);
            lineageCache.put(type, lineage);
            lineages.add(lineage);
        }

        return lineages;
    }
}
