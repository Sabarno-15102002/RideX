package com.ridex.driver.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DltReplayRunnerTest {

    @Mock
    private DltReplayOperations dltReplayOperations;

    private DltReplayRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DltReplayRunner(dltReplayOperations);
    }

    @Test
    void run_withValidTestId_shouldReplayRecord() throws Exception {
        var args = new DefaultApplicationArguments(
                "--test-id=test-123"
        );

        runner.run(args);

        verify(dltReplayOperations).replay("test-123");
    }

    @Test
    void run_withoutTestId_shouldThrowIllegalArgumentException() {
        var args = new DefaultApplicationArguments(new String[]{});

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> runner.run(args)
        );

        assertEquals(
                "Pass --test-id=<testId> to select the DLT record",
                exception.getMessage()
        );

        verifyNoInteractions(dltReplayOperations);
    }

    @Test
    void run_withBlankTestId_shouldThrowIllegalArgumentException() {
        var args = new DefaultApplicationArguments("--test-id=");

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.run(args)
        );

        verifyNoInteractions(dltReplayOperations);
    }

    @Test
    void run_whenReplayFails_shouldPropagateException() throws Exception {
        var args = new DefaultApplicationArguments(
                "--test-id=test-123"
        );

        doThrow(new IllegalStateException("Replay failed"))
                .when(dltReplayOperations)
                .replay("test-123");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.run(args)
        );

        assertEquals("Replay failed", exception.getMessage());
    }
}
