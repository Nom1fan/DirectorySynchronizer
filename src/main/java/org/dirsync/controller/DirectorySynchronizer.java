package org.dirsync.controller;

public interface DirectorySynchronizer extends Runnable {

    /**
     * Executes a new background thread to synchronize directories.
     * @throws org.dirsync.exception.DirectorySyncFailedException if the synchronization fails
     * @return the thread that is executing the synchronization
     */
    Thread syncDirectories() throws InterruptedException;

    boolean isRunning();

    boolean isFailed();
}
