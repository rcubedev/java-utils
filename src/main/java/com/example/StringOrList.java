package com.example;

import java.util.List;

import com.example.reflect.TypedClass;
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

    // public StringOrList(String value, TypedClass<String> clazz) {
    //     super(value, clazz);
    // }
    //
    // public StringOrList(List<String> values, TypedClass<String> clazz) {
    //     super(values, clazz);
    // }

    // @Override
    // protected @NotNull StringOrList newInstance(String value) {
    //     return new StringOrList(value);
    // }
    //
    // @Override
    // protected @NotNull StringOrList newInstance(List<String> values) {
    //     return new StringOrList(values);
    // }
}
