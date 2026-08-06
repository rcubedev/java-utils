package com.github.rcubedev.utils.event.impl.subscriber.linker.provider;

import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.methodhandle.DirectInstanceBindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.methodhandle.DirectStaticBindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.provider.binder.methodhandle.DirectWeakBindingFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MethodHandlesHandlerLinkerTest {

    private MethodHandlesHandlerLinker linker;
    private MethodHandles.Lookup lookup;

    @Mock private LinkageContext<TestEvent> mockContext;
    @Mock private AbstractMethodHandleLinker.UnreflectedContext mockUnreflected;

    @Mock private DirectInstanceBindingFactory.Provider mockInstanceProvider;
    @Mock private DirectStaticBindingFactory.Provider mockStaticProvider;
    @Mock private DirectWeakBindingFactory.Provider mockWeakProvider;

    @Mock private DirectInstanceBindingFactory<TestEvent> mockInstanceFactory;
    @Mock private DirectStaticBindingFactory<TestEvent> mockStaticFactory;
    @Mock private DirectWeakBindingFactory<TestEvent> mockWeakFactory;

    public static class SampleHandlerHost {
        public static void handleStatic(TestEvent event) {}
        public void handleInstance(TestEvent event) {}
    }

    @BeforeEach
    void setUp() {
        linker = new MethodHandlesHandlerLinker(mockInstanceProvider, mockStaticProvider, mockWeakProvider);
        lookup = MethodHandles.lookup();
    }

    @Nested
    class StaticMethods {

        @Test
        void testLinkStrongStaticMethodDelegatesToStaticProvider() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleStatic", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockContext.method()).thenReturn(method);
            when(mockUnreflected.isStatic()).thenReturn(true);
            when(mockUnreflected.handle()).thenReturn(handle);

            when(mockStaticProvider.create(eq(TestEvent.class), eq(method), any(MethodHandle.class)))
                    .thenReturn(mockStaticFactory);

            BindingFactory<TestEvent> result = linker.linkStrong(mockContext, mockUnreflected);

            assertSame(mockStaticFactory, result);
            verify(mockStaticProvider, times(1)).create(eq(TestEvent.class), eq(method), any(MethodHandle.class));
            verifyNoInteractions(mockInstanceProvider, mockWeakProvider);
        }
    }

    @Nested
    class InstanceMethods {

        @Test
        void testLinkStrongInstanceMethodDelegatesToInstanceProvider() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleInstance", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockContext.method()).thenReturn(method);
            when(mockUnreflected.isStatic()).thenReturn(false);
            when(mockUnreflected.handle()).thenReturn(handle);

            when(mockInstanceProvider.create(eq(TestEvent.class), eq(method), any(MethodHandle.class)))
                    .thenReturn(mockInstanceFactory);

            BindingFactory<TestEvent> result = linker.linkStrong(mockContext, mockUnreflected);

            assertSame(mockInstanceFactory, result);
            verify(mockInstanceProvider, times(1)).create(eq(TestEvent.class), eq(method), any(MethodHandle.class));
            verifyNoInteractions(mockStaticProvider, mockWeakProvider);
        }

        @Test
        void testLinkWeakInstanceMethodDelegatesToWeakProvider() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleInstance", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockContext.method()).thenReturn(method);
            when(mockUnreflected.handle()).thenReturn(handle);

            when(mockWeakProvider.create(eq(TestEvent.class), eq(method), any(MethodHandle.class)))
                    .thenReturn(mockWeakFactory);

            BindingFactory<TestEvent> result = linker.linkWeak(mockContext, mockUnreflected);

            assertSame(mockWeakFactory, result);
            verify(mockWeakProvider, times(1)).create(eq(TestEvent.class), eq(method), any(MethodHandle.class));
            verifyNoInteractions(mockInstanceProvider, mockStaticProvider);
        }
    }
}