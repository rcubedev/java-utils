package com.github.rcubedev.utils.event.impl.subscriber.linker;

import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.impl.subscriber.BindingFactory;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.MemberAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.ModuleAccessException;
import com.github.rcubedev.utils.event.impl.subscriber.linker.exception.StructuralLinkageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuntimeLinkageEngineTest {

    @Mock
    private LinkerEngine mockPrimary;

    @Mock
    private LinkerEngine mockFallback;

    @Mock
    private LinkageContext<TestEvent> mockContext;

    @Mock
    private BindingFactory<TestEvent> mockBinding;

    private RuntimeLinkageEngine engine;

    @BeforeEach
    void setUp() {
        // Inject the mocked collaborators using the package-private constructor
        this.engine = new RuntimeLinkageEngine(mockPrimary, mockFallback);
    }

    @Nested
    class StrongLinkage {

        @Test
        void testLinkStrongSuccessOnPrimary() throws Exception {
            when(mockPrimary.linkStrong(mockContext)).thenReturn(mockBinding);

            BindingFactory<TestEvent> result = engine.linkStrong(mockContext);

            assertSame(mockBinding, result);
            verify(mockPrimary).linkStrong(mockContext);
            verifyNoInteractions(mockFallback);
        }

        @Test
        void testLinkStrongFallbackOnPrimaryException() throws Exception {
            ModuleAccessException primaryException = new ModuleAccessException("Primary failed", null);
            when(mockPrimary.linkStrong(mockContext)).thenThrow(primaryException);
            when(mockFallback.linkStrong(mockContext)).thenReturn(mockBinding);

            BindingFactory<TestEvent> result = engine.linkStrong(mockContext);

            assertSame(mockBinding, result);
            verify(mockPrimary).linkStrong(mockContext);
            verify(mockFallback).linkStrong(mockContext);
        }

        @Test
        void testLinkStrongThrowsSuppressedExceptionWhenFallbackAlsoFails() throws Exception {
            StructuralLinkageException primaryEx = new StructuralLinkageException("Primary down", null);
            MemberAccessException fallbackEx = new MemberAccessException(new Exception("Fallback down"));

            when(mockPrimary.linkStrong(mockContext)).thenThrow(primaryEx);
            when(mockFallback.linkStrong(mockContext)).thenThrow(fallbackEx);

            StructuralLinkageException thrown = assertThrows(StructuralLinkageException.class, () -> engine.linkStrong(mockContext));

            assertSame(primaryEx, thrown);
            assertEquals(1, thrown.getSuppressed().length);
            assertSame(fallbackEx, thrown.getSuppressed()[0]);
        }
    }

    @Nested
    class WeakLinkage {

        @Test
        void testLinkWeakSuccessOnPrimary() throws Exception {
            when(mockPrimary.linkWeak(mockContext)).thenReturn(mockBinding);

            BindingFactory<TestEvent> result = engine.linkWeak(mockContext);

            assertSame(mockBinding, result);
            verify(mockPrimary).linkWeak(mockContext);
            verifyNoInteractions(mockFallback);
        }

        @Test
        void testLinkWeakFallbackOnPrimaryException() throws Exception {
            MemberAccessException primaryException = new MemberAccessException(new Exception("Primary security block"));
            when(mockPrimary.linkWeak(mockContext)).thenThrow(primaryException);
            when(mockFallback.linkWeak(mockContext)).thenReturn(mockBinding);

            BindingFactory<TestEvent> result = engine.linkWeak(mockContext);

            assertSame(mockBinding, result);
            verify(mockPrimary).linkWeak(mockContext);
            verify(mockFallback).linkWeak(mockContext);
        }

        @Test
        void testLinkWeakThrowsSuppressedExceptionWhenFallbackAlsoFails() throws Exception {
            ModuleAccessException primaryEx = new ModuleAccessException("Primary module error", null);
            StructuralLinkageException fallbackEx = new StructuralLinkageException("Fallback structural error", null);

            when(mockPrimary.linkWeak(mockContext)).thenThrow(primaryEx);
            when(mockFallback.linkWeak(mockContext)).thenThrow(fallbackEx);

            ModuleAccessException thrown = assertThrows(ModuleAccessException.class, () -> engine.linkWeak(mockContext));

            assertSame(primaryEx, thrown);
            assertEquals(1, thrown.getSuppressed().length);
            assertSame(fallbackEx, thrown.getSuppressed()[0]);
        }
    }
}