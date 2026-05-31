package com.github.rcubedev.example.event.impl.subscriber.linker.provider;

import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.LinkageContext;
import com.github.rcubedev.example.event.impl.subscriber.linker.exception.StructuralLinkageException;
import com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.metafactory.HandleBindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.metafactory.StaticBindingFactory;
import com.github.rcubedev.example.event.impl.subscriber.linker.provider.binder.metafactory.WeakBindingFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LmfHandlerLinkerTest {

    private LmfHandlerLinker linker;
    private MethodHandles.Lookup lookup;

    @Mock
    private LinkageContext<TestEvent> mockContext;

    @Mock
    private AbstractMethodHandleLinker.UnreflectedContext mockUnreflected;

    @Mock
    private HandleBindingFactory.Provider mockHandleProvider;

    @Mock
    private StaticBindingFactory.Provider mockStaticProvider;

    @Mock
    private WeakBindingFactory.Provider mockWeakProvider;

    @Mock
    private HandleBindingFactory<TestEvent> mockHandleFactory;

    @Mock
    private StaticBindingFactory<TestEvent> mockStaticFactory;

    @Mock
    private WeakBindingFactory<TestEvent> mockWeakFactory;

    public static class SampleHandlerHost {
        public static void handleStatic(TestEvent event) {}
        public void handleInstance(TestEvent event) {}
        public void handleMismatchedSignature(String badParam) {}
    }

    @BeforeEach
    void setUp() {
        linker = new LmfHandlerLinker(mockHandleProvider, mockStaticProvider, mockWeakProvider);
        lookup = MethodHandles.lookup();
    }

    @Nested
    class StaticMethods {

        @Test
        void testLinkStrongStaticMethodDelegatesToStaticProvider() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleStatic", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockUnreflected.isStatic()).thenReturn(true);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            // Updated mock alignment: expects Class and MethodHandle parameters
            when(mockStaticProvider.create(eq(TestEvent.class), any(MethodHandle.class))).thenReturn(mockStaticFactory);

            BindingFactory<TestEvent> result = linker.linkStrong(mockContext, mockUnreflected);

            assertSame(mockStaticFactory, result);
            verify(mockStaticProvider, times(1)).create(eq(TestEvent.class), any(MethodHandle.class));
            verifyNoInteractions(mockHandleProvider, mockWeakProvider);
        }
    }

    @Nested
    class InstanceMethods {

        @BeforeEach
        void setUpInstanceMocks() {
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(SampleHandlerHost.class);
        }

        @Test
        void testLinkStrongInstanceMethodDelegatesToHandleProvider() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleInstance", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockUnreflected.isStatic()).thenReturn(false);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            when(mockHandleProvider.create(eq(TestEvent.class), any(MethodHandle.class))).thenReturn(mockHandleFactory);

            BindingFactory<TestEvent> result = linker.linkStrong(mockContext, mockUnreflected);

            assertSame(mockHandleFactory, result);
            verify(mockHandleProvider, times(1)).create(eq(TestEvent.class), any(MethodHandle.class));
            verifyNoInteractions(mockStaticProvider, mockWeakProvider);
        }

        @Test
        void testLinkWeakInstanceMethodDelegatesToWeakProvider() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleInstance", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            // Updated mock alignment: expects Class and MethodHandle parameters
            when(mockWeakProvider.create(eq(TestEvent.class), any(MethodHandle.class))).thenReturn(mockWeakFactory);

            BindingFactory<TestEvent> result = linker.linkWeak(mockContext, mockUnreflected);

            assertSame(mockWeakFactory, result);
            verify(mockWeakProvider, times(1)).create(eq(TestEvent.class), any(MethodHandle.class));
            verifyNoInteractions(mockHandleProvider, mockStaticProvider);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void testLinkStrongThrowsStructuralLinkExceptionOnBadSignature() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleMismatchedSignature", String.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockUnreflected.isStatic()).thenReturn(false);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            assertThrows(StructuralLinkageException.class, () -> linker.linkStrong(mockContext, mockUnreflected));
            verifyNoInteractions(mockHandleProvider, mockStaticProvider, mockWeakProvider);
        }

        @Test
        void testLinkWeakThrowsStructuralLinkExceptionOnBadSignature() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleMismatchedSignature", String.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(SampleHandlerHost.class);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            assertThrows(StructuralLinkageException.class, () -> linker.linkWeak(mockContext, mockUnreflected));
            verifyNoInteractions(mockHandleProvider, mockStaticProvider, mockWeakProvider);
        }

        @Test
        void testLinkStrongPassesThroughBubbledStructuralExceptions() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleStatic", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            when(mockUnreflected.isStatic()).thenReturn(true);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            // Verifies the catch (StructuralLinkageException sle) pass-through path works cleanly
            when(mockStaticProvider.create(eq(TestEvent.class), any(MethodHandle.class)))
                    .thenThrow(new StructuralLinkageException("Bubbled down inside the constructor"));

            assertThrows(StructuralLinkageException.class, () -> linker.linkStrong(mockContext, mockUnreflected));
        }

        @Test
        void testLinkWeakPassesThroughBubbledStructuralExceptions() throws Throwable {
            Method method = SampleHandlerHost.class.getMethod("handleInstance", TestEvent.class);
            MethodHandle handle = lookup.unreflect(method);

            when(mockContext.paramType()).thenReturn(TestEvent.class);
            Mockito.<Class<?>>when(mockContext.targetClass()).thenReturn(SampleHandlerHost.class);
            when(mockUnreflected.lookup()).thenReturn(lookup);
            when(mockUnreflected.handle()).thenReturn(handle);

            when(mockWeakProvider.create(eq(TestEvent.class), any(MethodHandle.class)))
                    .thenThrow(new StructuralLinkageException("Bubbled down inside the constructor"));

            assertThrows(StructuralLinkageException.class, () -> linker.linkWeak(mockContext, mockUnreflected));
        }
    }
}