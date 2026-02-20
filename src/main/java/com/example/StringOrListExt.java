package com.example;

import java.util.List;

import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;

public class StringOrListExt extends StringOrList implements ConfigSerializableObject<Object> {

    // public StringOrListExt(String value) {
    //     super(value);
    // }
    //
    // public StringOrListExt(List<String> values) {
    //     super(values);
    // }

    public StringOrListExt(String value, Class<String> clazz) {
        super(value);
    }

    public StringOrListExt(List<String> values, Class<String> clazz) {
        super(values);
    }

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
