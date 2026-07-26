package com.github.rcubedev.example.event.api;

import org.junit.jupiter.api.Test;
import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.*;

class IdentityTests {

    @Test
    void of_ShouldCreateValidInstanceWithProvidedLookup() {
        MethodHandles.Lookup expectedLookup = MethodHandles.lookup();

        Identity identity = Identity.of(expectedLookup);

        assertNotNull(identity, "Factory method should return a non-null instance");
        assertSame(expectedLookup, identity.lookup(), "Getter must return the exact lookup reference passed to the factory");
    }

    @Test
    void of_ShouldThrowNullPointerExceptionWhenLookupIsNull() {
        NullPointerException exception = assertThrows(NullPointerException.class, 
                () -> Identity.of(null), 
                "Factory method must fail fast on a null lookup target");
        assertEquals("lookup cannot be null", exception.getMessage(), "Exception message must match constructor check validation text");
    }

    @Test
    void ofPublic_ShouldCreateValidInstanceWithPublicLookup() {
        Identity identity = Identity.ofPublic();

        assertNotNull(identity, "Public factory method should return a non-null instance");
        assertNotNull(identity.lookup(), "The held lookup reference inside the public identity must not be null");

        MethodHandles.Lookup canonicalPublic = MethodHandles.publicLookup();
        assertEquals(canonicalPublic.lookupClass(), identity.lookup().lookupClass());
        assertEquals(canonicalPublic.lookupModes(), identity.lookup().lookupModes());
        assertEquals(canonicalPublic.previousLookupClass(), identity.lookup().previousLookupClass());
    }
}
