package com.github.rcubedev.utils.config.elements;

import java.util.List;

import com.github.rcubedev.utils.reflect.TypedClass;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;

public class StringOrList extends ElementOrList<String> implements ConfigSerializableObject<Object> {

    private static final TypedClass<String> typedClass = new TypedClass<>() {};
    private static final TypedClass<List<String>> typedListClass = new TypedClass<>() {};
    public StringOrList(String value) {
        super(value, typedClass, typedListClass);
    }

    public StringOrList(List<String> values) {
        super(values, typedClass, typedListClass);
    }
}
