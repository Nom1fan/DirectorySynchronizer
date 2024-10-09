package org.dirsync.controller;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public interface DirectorySynchronizerV2 extends FileAlterationListener {

    /**
     * Begins listening for file changes in the source directory and synchronizes the target directory.
     */
    void start() throws InterruptedException;

    boolean isFailed();

    boolean isRunning();

    void stop();

    @Override
    default void onStart(FileAlterationObserver observer) {
        // Not needed
    }

    @Override
    default void onStop(FileAlterationObserver observer) {
        // Not needed
    }

    @Override
    default void onDirectoryCreate(File directory) {
        // Not needed
    }

    @Override
    default void onDirectoryChange(File directory) {
        // Not needed
    }

    @Override
    default void onDirectoryDelete(File directory) {
        // Not needed
    }

    @Override
    default void onFileChange(File file) {
        // Not needed
    }
}