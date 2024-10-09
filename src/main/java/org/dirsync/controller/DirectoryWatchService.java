package org.dirsync.controller;

import org.dirsync.controller.event.FileSystemEvent;
import org.dirsync.exception.DirectoryWatchFailedException;

import java.util.Set;

public interface DirectoryWatchService {

    /**
     * Register a directory to watch for file system events
     * @param directory the directory to watch
     * @throws DirectoryWatchFailedException if the directory cannot be watched
     */
    void registerRoot(String directory) throws DirectoryWatchFailedException;

    void registerSubDirectory(String subDirectory) throws DirectoryWatchFailedException;

    /**
     * Poll for file system events
     * @return a set of file system events
     * @throws InterruptedException if the thread is interrupted while waiting for events
     */
    Set<FileSystemEvent> pollEvents() throws InterruptedException;
}
