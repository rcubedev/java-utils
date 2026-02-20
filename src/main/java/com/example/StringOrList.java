package com.example;

import java.util.List;

import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;

public class StringOrList extends ElementOrList<String> implements ConfigSerializableObject<Object> {

    public StringOrList(String value) {
        super(value, String.class);
    }

    public StringOrList(List<String> values) {
        super(values, String.class);
    }

    // public StringOrList(String value, Class<String> clazz) {
    //     super(value, clazz);
    // }
    //
    // public StringOrList(List<String> values, Class<String> clazz) {
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
