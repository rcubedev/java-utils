import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class Test7 {

    static class MyClass {
        // Make the method public so it's accessible via findSpecial
        public void mymethod() {
            System.out.println("MyClass's mymethod");
        }
    }

    static class MySubclass extends MyClass {
        @Override
        public void mymethod() {
            System.out.println("MySubclass's mymethod");
        }
    }

    static class MySubSubclass extends MySubclass {
        @Override
        public void mymethod() {
            System.out.println("MySubSubclass's mymethod");
        }
    }

    public static void main(String[] args) throws Throwable {
        // Create an instance of MySubclass
        MySubSubclass subclassInstance = new MySubSubclass();

        // Step 1: Get the declared method from MyClass (superclass)
        Method method = MyClass.class.getDeclaredMethod("mymethod");

        // Step 2: Use MethodHandles.findSpecial to explicitly get the superclass method
        MethodHandle mh = MethodHandles.privateLookupIn(MyClass.class, MethodHandles.lookup()).findSpecial(
                MyClass.class,          // The class where the method is defined (superclass)
                "mymethod",             // Method name
                MethodType.methodType(void.class), // Method signature (no parameters, void return type)
                MyClass.class           // The class from which the method is being called (superclass)
        );

        // Step 3: Now invoke the method on the subclass instance
        mh.invokeExact((MyClass) subclassInstance);  // This will now call MyClass's mymethod, not MySubclass's
    }


}