package com.pulumi.provider.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComponentProviderHostTests {
    @Test
    void getEngineAddress_WithValidAddress_ReturnsAddress() {
        String[] args = new String[]{"127.0.0.1:51776"};
        assertEquals("127.0.0.1:51776", ComponentProviderHost.getEngineAddress(args));
    }

    @Test
    void getEngineAddress_WithLoggingArgs_ReturnsAddress() {
        String[] args = new String[]{
            "--logtostderr",
            "-v=3",
            "127.0.0.1:51776",
            "--logflow"
        };
        assertEquals("127.0.0.1:51776", ComponentProviderHost.getEngineAddress(args));
    }

    @Test
    void getEngineAddress_WithTracingArg_ReturnsAddress() {
        String[] args = new String[]{
            "--tracing",
            "1",
            "127.0.0.1:51776"
        };
        assertEquals("127.0.0.1:51776", ComponentProviderHost.getEngineAddress(args));
    }

    @Test
    void getEngineAddress_WithNoArgs_ThrowsException() {
        String[] args = new String[]{};
        var exception = assertThrows(IllegalArgumentException.class,
            () -> ComponentProviderHost.getEngineAddress(args));
        assertEquals("No engine address provided in arguments", exception.getMessage());
    }

    @Test
    void getEngineAddress_WithOnlyLoggingArgs_ThrowsException() {
        String[] args = new String[]{"--logtostderr", "-v=3", "--logflow"};
        var exception = assertThrows(IllegalArgumentException.class,
            () -> ComponentProviderHost.getEngineAddress(args));
        assertEquals("No engine address provided in arguments", exception.getMessage());
    }

    @Test
    void getEngineAddress_WithMultipleAddresses_ThrowsException() {
        String[] args = new String[]{
            "127.0.0.1:51776",
            "127.0.0.1:51777"
        };
        var exception = assertThrows(IllegalArgumentException.class,
            () -> ComponentProviderHost.getEngineAddress(args));
        assertEquals(
            "Expected exactly one engine address argument, but got 2 non-logging arguments",
            exception.getMessage()
        );
    }
} 