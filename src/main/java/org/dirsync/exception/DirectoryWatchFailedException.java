package org.dirsync.exception;

public class DirectoryWatchFailedException extends RuntimeException {

    public DirectoryWatchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
