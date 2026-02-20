import java.lang.reflect.Array;
import java.util.Arrays;

public class Test4 {

    @SafeVarargs // not really; just hiding warning
    static <T> T[] unsafeAsArray(T... args) {
        return args;
    }

    static <T> T[] unsafeArrayOfTwo(T a, T b) {
        return unsafeAsArray(a, b);
    }

    @SafeVarargs
    static <T> T[] safeAsArray(Class<T> clazz, T... args) {
        @SuppressWarnings("unchecked")
        Class<? extends T[]> arrayClass = (Class<? extends T[]>) Array.newInstance(clazz, args.length).getClass();
        return Arrays.copyOf(args, args.length, arrayClass);
    }

    static <T> T[] safeArrayOfTwo(Class<T> clazz, T a, T b) {
        return safeAsArray(clazz, a, b);
    }

    public static void main(String[] args) {
        // String[] bar = unsafeArrayOfTwo("hi", "mom");  // Crashes as unsafeArrayOfTwo would be erased to Object, Object
        // and thus the array would be erased to Object[]. This is an issue as you cannot cast Object[] -> String[] as arrays
        // know their component type at runtime, which would cannot be downcast to String, causing CCE.
        // Object[] bar2 = unsafeArrayOfTwo(1, 2);  // This would be unsafe

        // Safe way:
        String[] bar = safeArrayOfTwo(String.class, "hi", "mom");
    }
}
