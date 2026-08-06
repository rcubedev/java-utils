package com.github.rcubedev.example.config.elements;

import java.util.List;

import com.github.rcubedev.example.config.AllowConstructorFallback;
import com.github.rcubedev.example.config.serialization.AbstractElementOrList;
import com.github.rcubedev.example.reflect.TypedClass;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;

@AllowConstructorFallback
public class ElementOrList<T> extends AbstractElementOrList<T, ElementOrList<T>> implements ConfigSerializableObject<Object> {

    public ElementOrList(T value, TypedClass<T> type, TypedClass<List<T>> listType) {
        super(value, type, listType);
    }

    public ElementOrList(List<T> values, TypedClass<T> type, TypedClass<List<T>> listType) {
        super(values, type, listType);
    }
}
