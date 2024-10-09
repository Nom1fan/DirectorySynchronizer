package org.dirsync.exception;

public class DirectorySyncFailedException extends RuntimeException {

    public DirectorySyncFailedException(String message) {
        super(message);
    }

    public DirectorySyncFailedException(Throwable cause) {
        super("Directory synchronization failed.", cause);
    }

    public DirectorySyncFailedException(String message, Exception ex) {
        super(message, ex);
    }
}
