package org.dirsync.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.dirsync.util.RetryUtils.RetryException;
import static org.dirsync.util.RetryUtils.retryWithInterval;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryUtilsTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("retry.utils.interval.millis", "10");
    }

    @Test
    void testRetryWithInterval_SuccessOnFirstTry() throws InterruptedException {
        Runnable task = mock(Runnable.class);
        retryWithInterval(task, "Task failed");
        verify(task).run();
    }

    @Test
    void testRetryWithInterval_SuccessOnRetry() throws InterruptedException {
        Runnable task = mock(Runnable.class);
        doThrow(new RuntimeException("First attempt failed")).doNothing().when(task).run();
        retryWithInterval(task, "Task failed");
        verify(task, times(2)).run();
    }

    @Test
    void testRetryWithInterval_FailureAfterMaxRetries() {
        Runnable task = mock(Runnable.class);
        doThrow(new RuntimeException("All attempts failed")).when(task).run();

        RetryException exception = assertThrows(RetryException.class, () -> retryWithInterval(task, "Task failed"));

        assertEquals("Task failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        verify(task, times(3)).run();
    }

    @Test
    void testRetryWithInterval_CustomInterval() throws InterruptedException {
        Runnable task = mock(Runnable.class);
        doThrow(new RuntimeException("First attempt failed")).doNothing().when(task).run();
        retryWithInterval(task, "Task failed", 20);
        verify(task, times(2)).run();
    }
}