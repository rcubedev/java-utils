import java.lang.reflect.ParameterizedType;

public class Test2 {

    public static void main(String[] args) {
        Foo foo = new Foo();
        foo.doSomethingWithGenericTypeClass();
    }

    static class Foo extends AbstractFoo<GenericType> {

        public void doSomethingWithGenericTypeClass() {
            System.out.println("Java generic type is " + getJavaGenericType().getName());
            System.out.println("Spring generic type is " + getSpringGenericType().getName());
        }

    }

    static class AbstractFoo<T> {

        private final Class<T> javaGenericType;
        private final Class<T> springGenericType;

        @SuppressWarnings("unchecked")
        public AbstractFoo() {
            // Pure Java solution to get generic type
            Class<?> thisClass = getClass();
            javaGenericType = (Class<T>) ((ParameterizedType) thisClass.getGenericSuperclass())
                    .getActualTypeArguments()[0];

            // "Spring" solution (not needed but kept for consistency)
            this.springGenericType = javaGenericType;
        }

        protected Class<?> getJavaGenericType() {
            return this.javaGenericType;
        }

        protected Class<?> getSpringGenericType() {
            return this.springGenericType;
        }

    }

    static class GenericType {
        // Empty class as placeholder for generic type
    }
}
