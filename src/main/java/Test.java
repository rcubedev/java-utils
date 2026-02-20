// import java.lang.reflect.*;
// import java.util.List;
// import java.util.Map;
//
// public class Test {
//
//     public static void main(String[] args) throws NoSuchMethodException {
//         Class<?> clazz = MyClass.class;
//
//         System.out.println("Raw typeref: "+ new TypeReference<>(){}.getType().getTypeName());
//
//         // Define the expected generic types
//         Type[] expectedTypes = {
//                 new TypeReference<List<String>>(){}.getType(),
//                 new TypeReference<Map<Integer, Long>>(){}.getType(),
//         };
//
//         // Find the constructor with specific parameter types
//         Constructor<?> constructor = findConstructor(clazz, List.class, Map.class);
//
//         // Print the constructor found
//         System.out.println("Constructor found: " + constructor.toGenericString());
//
//         // Get the generic parameter types as Type objects
//         Type[] genericParameterTypes = constructor.getGenericParameterTypes();
//
//         // Check and print the generic types, also ensure they match the expected ones
//         for (int i = 0; i < genericParameterTypes.length; i++) {
//             Type parameterType = genericParameterTypes[i];
//             printGenericType(parameterType);
//
//             // Check if the parameter matches the expected generic type
//             if (!isMatchingGenericType(parameterType, expectedTypes[i])) {
//                 throw new RuntimeException("Constructor parameter " + i + " does not match the expected generic type.");
//             }
//         }
//
//         System.out.println("Constructor parameters match the expected types.");
//     }
//
//     // A generic class with a type parameter T
//     public static class MyClass<T> {
//         public MyClass(List<T> list, Map<Integer, T> map) {
//             // Constructor body
//         }
//     }
//
//     // Find the constructor with specific parameter types
//     public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
//         try {
//             return clazz.getConstructor(parameterTypes);
//         } catch (NoSuchMethodException e) {
//             throw new RuntimeException("No matching constructor found", e);
//         }
//     }
//
//     // Print the generic type of a given parameter type
//     public static void printGenericType(Type parameterType) {
//         if (parameterType instanceof ParameterizedType pType) {
//             System.out.println("Raw type: " + pType.getRawType().getTypeName());
//             for (Type typeArg : pType.getActualTypeArguments()) {
//                 System.out.println("  - Generic argument: " + typeArg.getTypeName());
//             }
//         }
//     }
//
//     // Check if the generic type matches the expected type
//     public static boolean isMatchingGenericType(Type parameterType, Type expectedType) {
//         if (parameterType instanceof ParameterizedType pType && expectedType instanceof ParameterizedType expectedPType) {
//             Type[] actualTypeArgs = pType.getActualTypeArguments();
//             Type[] expectedTypeArgs = expectedPType.getActualTypeArguments();
//
//             // Check if the number of type arguments match
//             if (actualTypeArgs.length != expectedTypeArgs.length) {
//                 return false;
//             }
//
//             // Check if each type argument matches
//             for (int i = 0; i < actualTypeArgs.length; i++) {
//                 if (!actualTypeArgs[i].equals(expectedTypeArgs[i])) {
//                     return false;
//                 }
//             }
//             return true;
//         }
//         return false; // If not ParameterizedType, they cannot match
//     }
//
//     // TypeReference helper class to capture generic type information
//     public abstract static class TypeReference<T> {
//         private final Type type;
//
//         protected TypeReference() {
//             Type superClass = getClass().getGenericSuperclass();
//             if (superClass instanceof Class<?>) { // sanity check
//                 throw new IllegalArgumentException("Missing type parameter. Please use: new TypeReference<MyType>() {}");
//             }
//             this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
//         }
//
//         public Type getType() {
//             return type;
//         }
//
//         public T noop() {
//             return null;
//         }
//     }
// }

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Test {

    // Interface for TypeReference and TypedClass
    public interface ITypeReference {
        Type getType();
        Type getRawType();
    }

    // Abstract base class to handle common type functionality
    public static abstract class AbstractTypeReference<T> implements ITypeReference {

        protected Type type;

        protected AbstractTypeReference() {}

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public Type getRawType() {
            ParameterizedType pType = getParameterizedTypeOrNull();
            if (pType == null) return type;
            return pType.getRawType();
        }

        protected ParameterizedType getParameterizedTypeOrNull() {
            if (type instanceof ParameterizedType pType) return pType;
            return null;
        }
    }

    // TypeReference for capturing parameterized types
    public static abstract class TypeReference<T> extends AbstractTypeReference<T> {

        protected TypeReference() {
            Type superClass = getClass().getGenericSuperclass();
            if (superClass instanceof Class<?>) { // if rawtypes used
                throw new IllegalArgumentException("Missing type parameter. Please use: new TypeReference<MyType>() {}");
            } else if (!(superClass instanceof ParameterizedType)) {
                throw new RuntimeException("Unexpected superclass type. Please ensure you are using a valid TypeReference subclass with proper generic type.");
            }
            this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        }
    }

    // TypedClass handles both parameterized types and primitives
    public static abstract class TypedClass<T> extends AbstractTypeReference<T> {

        private static final Map<Class<?>, TypedClass<?>> primitiveInstances = new ConcurrentHashMap<>();

        public TypedClass() {
            // Handle primitive types
            Type superClass = getClass().getGenericSuperclass();
            if (superClass instanceof ParameterizedType pType) {
                this.type = pType.getActualTypeArguments()[0];
            }
        }

        @SuppressWarnings("unchecked")
        public Class<T> getTypedClass() {
            return (Class<T>) getRawType();
        }

        public static TypedClass<?> ofPrimitive(Class<?> primitiveType) {
            if (!primitiveType.isPrimitive()) throw new IllegalArgumentException(primitiveType.getName() + " is not a primitive");
            return primitiveInstances.computeIfAbsent(primitiveType, key -> new PrimitiveTypedClass(primitiveType));
        }

        private static class PrimitiveTypedClass extends TypedClass {

            private final Class<?> classType;

            public PrimitiveTypedClass(Class<?> type) {
                this.type = type;  // Primitive type doesn't have generic types
                this.classType = type;
            }

            @Override
            public Class<?> getRawType() {
                return classType;
            }
        }
    }

    // Main method to demonstrate usage
    public static void main(String[] args) {
        // Example usage: Create TypedClass for List<String>
        TypedClass<List<String>> typedClass = new TypedClass<>() {};

        // Access the captured type
        System.out.println("Captured Type: " + typedClass.getType());

        // Access the Class<T> for the captured type
        Class<?> typeClass = typedClass.getTypedClass();
        System.out.println("Class Type: " + typeClass);

        // Example usage with a primitive type: int
        TypedClass<?> typedPrimClass = TypedClass.ofPrimitive(int.class);
        System.out.println("Prim Class Captured Type: " + typedPrimClass.getType());

        // Access the Class<T> for the primitive type
        Class<?> primClass = typedPrimClass.getTypedClass();
        System.out.println("Prim Class Type: " + primClass);
    }
}
