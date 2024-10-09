package org.dirsync.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RetryUtils {

    private static final int DEFAULT_RETRY_INTERVAL =
            Integer.parseInt(System.getProperty("retry.utils.interval.millis", "1000"));
    private static final int MAX_NUM_RETRIES = 3;

    public void retryWithInterval(Runnable task, String errorMessage) {
        retryWithInterval(task, errorMessage, DEFAULT_RETRY_INTERVAL);
    }

    public void retryWithInterval(Runnable task, String errorMessage, int retryInterval) {
        int numRetries = 0;
        Exception lastException = null;
        while (numRetries < MAX_NUM_RETRIES) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                lastException = e;
                numRetries++;
                sleep(retryInterval);
            }
        }
        throw new RetryException(lastException, errorMessage);
    }

    private static void sleep(int retryInterval) {
        try {
            Thread.sleep(retryInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class RetryException extends RuntimeException {
        public RetryException(Throwable throwable, String message) {
            super(message, throwable);
        }
    }
}
