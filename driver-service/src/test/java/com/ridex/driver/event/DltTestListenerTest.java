package com.ridex.driver.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DltTestListenerTest {

    private final DltTestListener listener = new DltTestListener();

    @Test
    void listen_shouldThrowIntentionalFailure() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> listener.listen("test-message"));

        assertEquals(
                "Intentional failure for DLT verification",
                exception.getMessage());
    }
}
