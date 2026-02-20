import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Test3<T extends Number & Comparable<T> & Serializable> {

    public static void main(String[] args) {
        for (Constructor<?> ctor : Test3.class.getDeclaredConstructors()) {
            Type[] genericParams = ctor.getGenericParameterTypes();

            System.out.println("Constructor found: " + ctor + ". Var args: " + ctor.isVarArgs());
            System.out.println("Generic ctor: " + ctor.toGenericString());
            System.out.println("Generic param types: " + Arrays.toString(genericParams));

            for (Type type : genericParams) {
                System.out.println("  Type: " + type.getTypeName());
                System.out.println("  Class type: " + type.getClass());

                // Handle different type cases
                handleType(type);
            }

            System.out.println();
        }
    }

    // Refactored handling logic to be dynamic
    private static void handleType(Type type) {
        switch (type) {
            case ParameterizedType pt -> handleParameterizedType(pt);
            case TypeVariable<?> tv -> handleTypeVariable(tv);
            case Class<?> cls -> handleClassType(cls);
            case GenericArrayType gat -> handleGenericArrayType(gat);
            case null, default -> throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    // Handle ParameterizedType
    private static void handleParameterizedType(ParameterizedType pt) {
        System.out.println("    Raw type: " + pt.getRawType());
        Type[] actualTypeArgs = pt.getActualTypeArguments();
        System.out.println("    Actual type arguments: " + Arrays.toString(actualTypeArgs));
        for (Type actualTypeArg : actualTypeArgs) {
            if (actualTypeArg instanceof WildcardType wildcardType) {
                System.out.println("      Wildcard upper bounds: " + Arrays.toString(wildcardType.getUpperBounds()));
                System.out.println("      Wildcard lower bounds: " + Arrays.toString(wildcardType.getLowerBounds()));
            } else {
                System.out.println("      Actual type argument: " + actualTypeArg.getTypeName());
            }
        }
    }

    // Handle TypeVariable
    private static void handleTypeVariable(TypeVariable<?> tv) {
        System.out.println("    Type variable name: " + tv.getName());
        System.out.println("    Bounds: " + Arrays.toString(tv.getBounds()));
        for (Type bound : tv.getBounds()) {
            System.out.println("    Bound type: " + bound.getTypeName());
            if (bound instanceof ParameterizedType pType) {
                System.out.println("      Bound: " + Arrays.toString(pType.getActualTypeArguments()));
            }
        }
    }

    // Handle Class type
    private static void handleClassType(Class<?> cls) {
        System.out.println("    Raw class: " + cls.getName());
    }

    // Handle GenericArrayType
    private static void handleGenericArrayType(GenericArrayType gat) {
        System.out.println("    Generic Array Type: " + gat.getTypeName());
        Type componentType = gat.getGenericComponentType();
        System.out.println("    Component type: " + componentType.getTypeName());
        handleType(componentType);  // Recursively handle the component type
    }

    // Constructors with different parameter types
    public Test3(T tVal, List<T> tList, String string) {}

    public Test3(T tVal, List<? extends List<T>> tList, int integer) {}

    public Test3(T tVal, Collection<? super List<T>> tList, int integer) {}

    @SafeVarargs
    public Test3(T tVal, T... tVarArgs) {}

    public Test3(T tVal, T[] tArr, Void vo1d) {}
}
