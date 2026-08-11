package com.github.rcubedev.utils.event.impl.subscriber.linker;

import com.github.rcubedev.utils.event.api.*;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.annotation.Weak;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.HandlerFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.StructuralLinkageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MethodLinkerTest {

    @Mock
    private LinkerEngine mockEngine;

    @Mock
    private BindingFactory<TestEvent> mockBinding;

    @Mock
    private LinkageContext<TestEvent> mockContext;

    @Mock
    private HandlerFactory.Provider mockHandlerFactoryProvider;

    @Mock
    private HandlerFactory<TestEvent> mockHandlerFactory;

    private Method normalMethod;
    private Method weakMethod;
    private Method staticWeakMethod;
    private Method methodInWeakClass;

    public static class FakeListener {
        @SubscribeEvent(priority = Priority.MONITOR, ignoreCancelled = true)
        public void normalMethod(TestEvent event) {}

        @SubscribeEvent
        @Weak
        public void weakMethod(TestEvent event) {}

        @SubscribeEvent
        @Weak
        public static void staticWeakMethod(TestEvent event) {}
    }

    @Weak
    public static class FakeWeakClassListener {
        @SubscribeEvent
        public void methodInWeakClass(TestEvent event) {}
    }

    @BeforeEach
    void setUp() throws Exception {
        normalMethod = FakeListener.class.getMethod("normalMethod", TestEvent.class);
        weakMethod = FakeListener.class.getMethod("weakMethod", TestEvent.class);
        staticWeakMethod = FakeListener.class.getMethod("staticWeakMethod", TestEvent.class);
        methodInWeakClass = FakeWeakClassListener.class.getMethod("methodInWeakClass", TestEvent.class);
    }

    @Test
    void testCompileStrongHandler() throws Exception {
        when(mockContext.method()).thenReturn(normalMethod);
        when(mockEngine.linkStrong(mockContext)).thenReturn(mockBinding);
        when(mockHandlerFactoryProvider.create(eq(Priority.MONITOR), eq(true), eq(mockBinding)))
                .thenReturn(mockHandlerFactory);

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeListener.class, false, mockEngine, mockContext, mockHandlerFactoryProvider);
        HandlerFactory<TestEvent> result = linker.compile();

        assertSame(mockHandlerFactory, result);
        verify(mockEngine).linkStrong(mockContext);
        verify(mockEngine, never()).linkWeak(any());
        verify(mockHandlerFactoryProvider).create(Priority.MONITOR, true, mockBinding);
    }

    @Test
    void testCompileWeakHandlerFromMethodAnnotation() throws Exception {
        when(mockContext.method()).thenReturn(weakMethod);
        when(mockEngine.linkWeak(mockContext)).thenReturn(mockBinding);
        when(mockHandlerFactoryProvider.create(any(), anyBoolean(), eq(mockBinding)))
                .thenReturn(mockHandlerFactory);

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeListener.class, false, mockEngine, mockContext, mockHandlerFactoryProvider);
        HandlerFactory<TestEvent> result = linker.compile();

        assertSame(mockHandlerFactory, result);
        verify(mockEngine).linkWeak(mockContext);
        verify(mockEngine, never()).linkStrong(any());
    }

    @Test
    void testCompileWeakHandlerFromClassAnnotation() throws Exception {
        when(mockContext.method()).thenReturn(methodInWeakClass);
        when(mockEngine.linkWeak(mockContext)).thenReturn(mockBinding);
        when(mockHandlerFactoryProvider.create(any(), anyBoolean(), eq(mockBinding)))
                .thenReturn(mockHandlerFactory);

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeWeakClassListener.class, false, mockEngine, mockContext, mockHandlerFactoryProvider);
        HandlerFactory<TestEvent> result = linker.compile();

        assertSame(mockHandlerFactory, result);
        verify(mockEngine).linkWeak(mockContext);
        verify(mockEngine, never()).linkStrong(any());
    }

    @Test
    void testStaticWeakMethodThrowsIllegalArgumentException() {
        when(mockContext.method()).thenReturn(staticWeakMethod);

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeListener.class, true, mockEngine, mockContext, mockHandlerFactoryProvider);

        assertThrows(IllegalArgumentException.class, linker::compile);
        verifyNoInteractions(mockEngine);
        verifyNoInteractions(mockHandlerFactoryProvider);
    }

    @Test
    void testWrapsModuleAccessException() throws Exception {
        when(mockContext.method()).thenReturn(normalMethod);
        when(mockEngine.linkStrong(mockContext)).thenThrow(new ModuleAccessException("Module error"));

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeListener.class, false, mockEngine, mockContext, mockHandlerFactoryProvider);

        assertThrows(IllegalArgumentException.class, linker::compile);
        verifyNoInteractions(mockHandlerFactoryProvider);
    }

    @Test
    void testWrapsMemberAccessException() throws Exception {
        when(mockContext.method()).thenReturn(normalMethod);
        when(mockEngine.linkStrong(mockContext)).thenThrow(new MemberAccessException(new Exception("Visibility error")));

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeListener.class, false, mockEngine, mockContext, mockHandlerFactoryProvider);

        assertThrows(IllegalArgumentException.class, linker::compile);
        verifyNoInteractions(mockHandlerFactoryProvider);
    }

    @Test
    void testWrapsStructuralLinkageException() throws Exception {
        when(mockContext.method()).thenReturn(normalMethod);
        when(mockEngine.linkStrong(mockContext)).thenThrow(new StructuralLinkageException("Linkage error"));

        MethodLinker<TestEvent> linker = new MethodLinker<>(FakeListener.class, false, mockEngine, mockContext, mockHandlerFactoryProvider);

        assertThrows(RuntimeException.class, linker::compile);
        verifyNoInteractions(mockHandlerFactoryProvider);
    }
}