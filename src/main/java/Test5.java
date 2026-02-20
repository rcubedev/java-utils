import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class Test5 {

    public static void main(String[] args) {
        record Foo(int[] ints){}

        var ints = new int[]{1, 2};
        var foo = new Foo(ints);
        C.getCaller();
        // System.out.println(Modifier.isStatic(foo.getClass().getModifiers())); // true
        // System.out.println(foo); // Foo[ints=[I@xxxx]
        // System.out.println(new Foo(new int[]{1,2}).equals(new Foo(new int[]{1,2}))); // false
        // System.out.println(new Foo(ints).equals(new Foo(ints))); //true
        // System.out.println(foo.equals(foo)); // true
    }

    public static class A {
        public static void getCaller() {
            System.out.println("Caller class: " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        }
    }

    public static class B {
        public static void getCaller() {
            A.getCaller();
        }
    }

    public static class C {
        public static void getCaller() {
            B.getCaller();
        }
    }
}
