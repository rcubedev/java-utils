package com.example;

import java.util.List;

import com.example.reflect.TypedClass;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;

@AllowConstructorFallback
public class ElementOrList<T> extends AbstractElementOrList<T, ElementOrList<T>> implements ConfigSerializableObject<Object> {

    public ElementOrList(T value, TypedClass<T> type) {
        super(value, type);
    }

    public ElementOrList(List<T> values, TypedClass<T> type) {
        super(values, type);
    }

    // @Override
    // protected @NotNull ElementOrList<T> newInstance(T value) {
    //     return new ElementOrList<>(value, getType());
    // }
    //
    // @Override
    // protected @NotNull ElementOrList<T> newInstance(List<T> values) {
    //     return new ElementOrList<>(values, getType());
    // }
}
