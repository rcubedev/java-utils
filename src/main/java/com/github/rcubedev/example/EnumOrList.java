package com.github.rcubedev.example;

import com.github.rcubedev.example.reflect.TypedClass;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllowConstructorFallback
public class EnumOrList<T extends Enum<T> & ISerializableEnum<T>> extends ElementOrList<T> implements ConfigSerializableObject<Object> {

    public EnumOrList(T value, TypedClass<T> typedEnum, TypedClass<List<T>> typedList) {
        super(value, typedEnum, typedList);
    }

    public EnumOrList(List<T> values, TypedClass<T> typedEnum, TypedClass<List<T>> typedList) {
        super(values, typedEnum, typedList);
    }

    // can't do this logic in a ctor as can't pick which this(...) ctor to call; must be first. subclasses should hide this method so the right runtime type is given
    public static <T extends Enum<T> & ISerializableEnum<T>> EnumOrList<T> of(@NotNull String value, @NotNull TypedClass<T> typedEnum, @NotNull TypedClass<List<T>> typedList) {
        List<T> enums = parseEnumList(value, typedEnum);
        if (enums.isEmpty()) {
            throw new IllegalArgumentException(constructEnumErrorMessage(value, typedEnum));
        }
        return (enums.size() == 1) ? new EnumOrList<>(enums.getFirst(), typedEnum, typedList) : new EnumOrList<>(enums, typedEnum, typedList);
    }

    private static <T extends Enum<T> & ISerializableEnum<T>> List<T> parseEnumList(@NotNull String representation, @NotNull TypedClass<T> typedEnum) {
        if ("*".equals(representation)) {
            T[] enumConstants = typedEnum.getTypedClass().getEnumConstants();
            if (enumConstants.length == 0) {
                throw new IllegalStateException(String.format("Enum %s has no constants. Unable to parse: %s", typedEnum, representation));
            }
            return Arrays.asList(enumConstants);
        }
        try {
            return List.of(Enum.valueOf(typedEnum.getTypedClass(), representation));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    // Extracting exception message construction logic to a helper method
    private static <T extends Enum<T> & ISerializableEnum<T>> String constructEnumErrorMessage(@NotNull String value, @NotNull TypedClass<T> typedEnum) {
        String enumListString = Arrays.stream(typedEnum.getTypedClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return String.format("%s is not * or a valid enum constant. Allowed values: [*, %s]", value, enumListString);
    }

    @Override
    public EnumOrList<T> convertFrom(Object representation) {
        if (representation instanceof String str) {
            List<T> enums = parseEnumList(str, getType());
            if (enums.isEmpty()) {
                throw new IllegalStateException(constructEnumErrorMessage(str, getType()));
            }
            return enums.size() == 1 ? (EnumOrList<T>) newInstance(enums.getFirst()) : (EnumOrList<T>) newInstance(enums);
        }
        return (EnumOrList<T>) super.convertFrom(representation);
    }
}
