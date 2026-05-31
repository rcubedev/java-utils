package com.github.rcubedev.example.event.impl.subscriber.linker.provider;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.StubMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractMethodHandleLinkerTest {

    private TestLinker linker;
    private MethodHandles.Lookup lookup;

    @Mock
    private LinkageContext<TestEvent> mockContext;

    @Mock
    private BindingFactory<TestEvent> mockBindingFactory;

    public static class SampleTarget {
        public static void publicStaticMethod(TestEvent event) {}
        private void privateInstanceMethod(TestEvent event) {}
    }

    @BeforeEach
    void setUp() {
        linker = new TestLinker(mockBindingFactory);
        lookup = MethodHandles.lookup();
    }

    @Nested
    class SuccessfulLinkage {

        @Test
        void testLinkStrongUnreflectsAndCoordinatesHooks() throws Exception {
            Method method = SampleTarget.class.getMethod("publicStaticMethod", TestEvent.class);

            when(mockContext.lookup()).thenReturn(lookup);
            when(mockContext.method()).thenReturn(method);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(SampleTarget.class);

            BindingFactory<TestEvent> result = linker.linkStrong(mockContext);

            assertSame(mockBindingFactory, result);
            assertNotNull(linker.lastUnreflected);
            assertTrue(linker.lastUnreflected.isStatic());
            assertNotNull(linker.lastUnreflected.handle());

            // Verifies the lookup successfully positioned itself into the target class package
            assertEquals(SampleTarget.class, linker.lastUnreflected.lookup().lookupClass());
        }

        @Test
        void testLinkWeakUnreflectsAndCoordinatesHooks() throws Exception {
            Method method = SampleTarget.class.getMethod("publicStaticMethod", TestEvent.class);

            when(mockContext.lookup()).thenReturn(lookup);
            when(mockContext.method()).thenReturn(method);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(SampleTarget.class);

            BindingFactory<TestEvent> result = linker.linkWeak(mockContext);

            assertSame(mockBindingFactory, result);
            assertNotNull(linker.lastUnreflected);
        }
    }

    @Nested
    class AccessExceptionHandling {

        @Test
        void testPrepareContextThrowsMemberAccessExceptionOnPrivateMethod() throws Exception {
            // unreflecting a private method using a lookup positioned in a different class context
            // naturally throws an IllegalAccessException.
            Method method = SampleTarget.class.getDeclaredMethod("privateInstanceMethod", TestEvent.class);

            // Force the lookup out of its friendly nest environment into a foreign context (String)
            MethodHandles.Lookup standardLookup = MethodHandles.publicLookup().in(SampleTarget.class);

            when(mockContext.lookup()).thenReturn(standardLookup);
            when(mockContext.method()).thenReturn(method);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(SampleTarget.class);

            // The modules can read each other (same module), meaning it will pass the module guard
            // and throw a MemberAccessException exactly as expected.
            assertThrows(MemberAccessException.class, () -> linker.linkStrong(mockContext));
        }

        @Test
        void testPrepareContextThrowsModuleAccessExceptionOnUnexportedModulePackage() throws Exception {
            final String mod = "com.github.rcubedev.dynamic.unexported";
            Class<?> target = generateDynamicModuleTarget(mod);
            Method method = target.getMethod("publicStaticMethod", TestEvent.class);

            // Using our test's standard full-power lookup (unnamed module).
            // It can read the dynamic module, but the package is not exported back to it.
            when(mockContext.lookup()).thenReturn(lookup);
            when(mockContext.method()).thenReturn(method);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(target);

            assertThrows(ModuleAccessException.class, () -> linker.linkStrong(mockContext));
        }

        @Test
        void testPrepareContextThrowsModuleAccessExceptionOnUnreadableModule() throws Exception {
            final String mod = "com.github.rcubedev.dynamic.unreadable";
            Class<?> target = generateDynamicModuleTarget(mod);
            Method method = target.getMethod("publicStaticMethod", TestEvent.class);

            MethodHandles.Lookup unreadableLookup = MethodHandles.publicLookup().in(HttpClient.class);

            when(mockContext.lookup()).thenReturn(unreadableLookup);
            when(mockContext.method()).thenReturn(method);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(target);

            assertThrows(ModuleAccessException.class, () -> linker.linkStrong(mockContext));
        }

        private Class<?> generateDynamicModuleTarget(String moduleName) throws Exception {
            Map<String, byte[]> resources = new HashMap<>();

            resources.put("module-info.class", new ByteBuddy()
                    .makeModule(moduleName)
                    .make()
                    .getBytes());

            resources.put(moduleName.replace('.', '/') + "/EncapsulatedTarget.class", new ByteBuddy()
                    .subclass(Object.class)
                    .name(moduleName + ".EncapsulatedTarget")
                    .defineMethod("publicStaticMethod", void.class, Modifier.PUBLIC | Modifier.STATIC)
                    .withParameters(TestEvent.class)
                    .intercept(StubMethod.INSTANCE)
                    .make()
                    .getBytes());

            ModuleFinder finder = new ModuleFinder() {
                final ModuleReference ref = new ModuleReference(
                        ModuleDescriptor.newModule(moduleName).packages(Set.of(moduleName)).build(), null) {
                    @Override
                    public ModuleReader open() {
                        return new ModuleReader() {
                            @Override public Optional<URI> find(String name) { return Optional.empty(); }
                            @Override public Optional<ByteBuffer> read(String name) {
                                return Optional.ofNullable(resources.get(name)).map(ByteBuffer::wrap);
                            }
                            @Override public Stream<String> list() { return resources.keySet().stream(); }
                            @Override public void close() {}
                        };
                    }
                };
                @Override public Optional<ModuleReference> find(String name) {
                    return moduleName.equals(name) ? Optional.of(ref) : Optional.empty();
                }
                @Override public Set<ModuleReference> findAll() { return Set.of(ref); }
            };

            ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(
                    ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), Set.of(moduleName)),
                    getClass().getClassLoader());

            return layer.findLoader(moduleName).loadClass(moduleName + ".EncapsulatedTarget");
        }
    }
    /**
     * Test verification stub to track parameters passing down into abstract hook implementations.
     */
    private static final class TestLinker extends AbstractMethodHandleLinker {

        private final BindingFactory<?> factoryStub;
        private UnreflectedContext lastUnreflected;

        TestLinker(BindingFactory<?> factoryStub) {
            this.factoryStub = factoryStub;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends Event> BindingFactory<T> linkStrong(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
            this.lastUnreflected = unreflected;
            return (BindingFactory<T>) factoryStub;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends Event> BindingFactory<T> linkWeak(LinkageContext<T> context, UnreflectedContext unreflected) throws StructuralLinkageException {
            this.lastUnreflected = unreflected;
            return (BindingFactory<T>) factoryStub;
        }
    }
}