package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;

import java.util.*;

public class FamilyBuilder<B extends Event> {

    private final RegistrySnapshot<B> snapshot;
    private final RegisteredParentResolver<B> resolver;

    public FamilyBuilder(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver) {
        this.snapshot = snapshot;
        this.resolver = resolver;
    }

    /**
     * Build families from event types sorted by hierarchy depth.
     * <p>
     * A family is a linear chain; a new family starts at a branch point.
     */
    public List<List<Class<? extends B>>> buildFamilies() {
        List<Class<? extends B>> sortedTypes = new ArrayList<>(snapshot.getHandlers().keySet());
        sortedTypes.sort(Comparator.comparingInt(resolver::hierarchyDepth));

        // Families are just linear chains where each child's parent is the element before it.
        List<List<Class<? extends B>>> families = new ArrayList<>();
        Map<Class<? extends B>, Integer> typeToFamilyIdx = new HashMap<>();

        for (Class<? extends B> type : sortedTypes) {
            Class<? extends B> parent = resolver.getRegisteredParentAsExtendsBus(type);

            if (parent == null || !typeToFamilyIdx.containsKey(parent)) {
                List<Class<? extends B>> family = new ArrayList<>();
                family.add(type);
                typeToFamilyIdx.put(type, families.size());
                families.add(family);
            } else {
                int familyIdx = typeToFamilyIdx.get(parent);
                List<Class<? extends B>> family = families.get(familyIdx);

                if (family.getLast().equals(parent)) {
                    family.add(type);
                    typeToFamilyIdx.put(type, familyIdx);
                } else {
                    // Sibling branch: Starts a NEW family.
                    // IMPORTANT: This family starts at the sibling, NOT the parent.
                    List<Class<? extends B>> newBranch = new ArrayList<>();
                    newBranch.add(type);
                    typeToFamilyIdx.put(type, families.size());
                    families.add(newBranch);
                }
            }
        }

        return families;
    }
}
