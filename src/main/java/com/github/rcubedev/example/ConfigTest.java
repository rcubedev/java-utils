package com.github.rcubedev.example;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.DisplayNameConvention;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.SerializedNameConvention;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.NamingSchemes;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ConfigSerializableObject;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import org.jetbrains.annotations.Nullable;

@DisplayNameConvention(NamingSchemes.SPACE_SEPARATED_LOWER_CASE_INITIAL_UPPER_CASE)
@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class ConfigTest extends WrappedConfig {
    @Comment({"Line1?", "Line2?", "", "DO NOT change this value??"})
    @IntegerRange(min = 1, max = 1000)
    public int configVer = 2;

    @Comment("Random Idk edit")
    public int random = 5;

    @Comment("custom type tests")
    public CustomTypeTest test_value_1 = CustomTypeTest.itemALL;
    public CustomTypeTest test_value_2 = new CustomTypeTest(new String[]{"dminecraft:overworld", "example:custom_dimension"});

    @Comment({"This is a list of ignored dimensions for each world that CombatLogX will follow when tagging players.",
            "World names are case-sensitive. \"world\" is not the same as \"WoRlD\"",
            "Make sure you are not using the world aliases from Multiverse",
            "",
            "None disabled example (default)",
            "disabled-world-list = {}",
            "",
            "Example with some dimensions disabled",
            "[disabled_worlds]:",
            "\"disabled_world_1\" = \"ALL\" <-- CombatLogX is disabled for all dimensions in disabled_world_1",
            "\"DiSaBlEd_WoRlD_2\" = [\"minecraft:overworld\", \"example:custom_dimension\"] <-- CombatLogX is disabled for the overworld and a custom dimension in DiSaBlEd_WoRlD_2"
    })
    public Map<String, CustomTypeTest> disabled_worlds = ValueMap.builder(new CustomTypeTest("ALL"))
            .build();

    public static class CustomTypeTest implements ConfigSerializableObject<Object> {
        private static final String ALL = "ALL"; // compiler inlined
        public boolean isAll;
        @Nullable public ValueList<String> dimensions;

        protected static final CustomTypeTest itemALL = new CustomTypeTest(ALL);

        // constructor for ALL
        public CustomTypeTest(String all) {
            if (!all.equals(ALL)) throw new IllegalArgumentException("Only '" + ALL + "' allowed for string constructor");
            this.isAll = true;
            this.dimensions = null;
        }

        // constructor for list
        public CustomTypeTest(String[] dims) {
            this.isAll = false;
            this.dimensions = ValueList.create("", dims);
        }

        @Override
        public CustomTypeTest convertFrom(Object representation) {
            String strRepresentation = StringHelper.toString(representation);
            if (strRepresentation != null) return new CustomTypeTest(strRepresentation);

            else if (representation instanceof List<?> list) {
                if (list.stream().allMatch(String.class::isInstance)) {
                    @SuppressWarnings("unchecked")
                    List<String> stringList = (List<String>) representation; // Safe cast as we know all elements are strings & string is final
                    String[] strArray = stringList.toArray(new String[0]);
                    return new CustomTypeTest(strArray);
                }

                String[] strArray = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    String itemString = StringHelper.toStringExcludingNull(item);

                    // Handle boxed primitives and null-safe toString conversion
                    if (itemString == null) {
                        throw new IllegalArgumentException("ValueList contains non stringable elements: " + representation);
                    } else {
                        strArray[i] = itemString;
                    }
                }

                return new CustomTypeTest(strArray);
            }
            throw new IllegalArgumentException("Invalid representation: " + representation);
        }

        @Override
        public Object getRepresentation() {
            return isAll ? ALL : dimensions;
        }

        @Override
        public CustomTypeTest copy() {
            if (isAll) return new CustomTypeTest(ALL);
            return new CustomTypeTest(Objects.requireNonNull(dimensions).toArray(new String[0])); // shouldn't be null
        }

        @Override
        public String toString() {
            Object representation = getRepresentation();
            String strRepresentation = StringHelper.toString(representation);
            if (strRepresentation != null) return strRepresentation;
            else if (representation instanceof Collection<?> collection) return collectionToString(collection);
            throw new IllegalArgumentException("Invalid representation: " + representation);
        }

        // TODO :: Make Collection util class
        // Taken from AbstractCollection, used as ValueList doesn't extend AbstractCollection/AbstractList leading to bad output
        public <E> String collectionToString(Collection<E> list) {
            Iterator<E> it = list.iterator();
            if (!it.hasNext())
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (;;) {
                E e = it.next();
                sb.append(e == list ? "(this Collection)" : e);
                if (! it.hasNext())
                    return sb.append(']').toString();
                sb.append(',').append(' ');
            }
        }
    }
}
