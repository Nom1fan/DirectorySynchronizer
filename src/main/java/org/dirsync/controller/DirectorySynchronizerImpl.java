package org.dirsync.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.dirsync.exception.DirectorySyncFailedException;
import org.dirsync.exception.DirectoryWatchFailedException;
import org.dirsync.input.SyncDirectoriesValidator;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.dirsync.model.file.SyncFile;
import org.dirsync.model.file.SyncFileFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DirectorySynchronizerImpl implements DirectorySynchronizer {

    private final SyncDirectoriesInfo syncDirectoriesInfo;
    private final FileAlterationMonitor fileAlterationMonitor;
    private final SyncFileFactory syncFileFactory;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean failed = false;
    private static final int DIR_SYNC_MAX_NUM_RETIRES = Integer.parseInt(System.getProperty("dir.sync.max.num.retries", "3"));
    private int dirSyncNumRetries = 0;

    public DirectorySynchronizerImpl(SyncDirectoriesInfo syncDirectoriesInfo,
                                     FileAlterationMonitor fileAlterationMonitor, SyncFileFactory syncFileFactory) {
        this.syncDirectoriesInfo = syncDirectoriesInfo;
        this.fileAlterationMonitor = fileAlterationMonitor;
        this.syncFileFactory = syncFileFactory;
        SyncDirectoriesValidator.validate(syncDirectoriesInfo);
    }

    @Override
    public void start() {
        try {
            monitorDirectory(syncDirectoriesInfo.sourceDirPath());
            running.set(true);
            log.info("Synchronizing directories: " + syncDirectoriesInfo);
        } catch (Exception e) {
            throw new DirectoryWatchFailedException("Failed to monitor directory", e);
        }
    }

    private void monitorDirectory(String directoryPath) throws Exception {
        FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(directoryPath);
        fileAlterationObserver.addListener(this);
        fileAlterationMonitor.addObserver(fileAlterationObserver);
        fileAlterationMonitor.start();
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void setFailed() {
        failed = true;
    }

    private void stopAndFailIfMaxAttemptsReached(Exception e) {
        dirSyncNumRetries++;
        if (maxRetriesReached()) {
            log.error("Directory synchronization failed after {} retries", DIR_SYNC_MAX_NUM_RETIRES, e);
            setFailed();
            stop();
            throw new DirectorySyncFailedException(e);
        }
        log.error("Directory synchronization failed. Recover attempt {}/{}", dirSyncNumRetries, DIR_SYNC_MAX_NUM_RETIRES, e);
    }

    private boolean maxRetriesReached() {
        return dirSyncNumRetries > DIR_SYNC_MAX_NUM_RETIRES;
    }

    @Override
    public void stop() {
        try {
            fileAlterationMonitor.stop();
            running.set(false);
        } catch (Exception e) {
            throw new DirectoryWatchFailedException("Failed to stop file alteration monitor", e);
        }
    }

    @Override
    public void onFileCreate(File file) {
        try {
            syncFileCreated(file.toPath());
        } catch (FileAlreadyExistsException e) {
            log.warn("File: {} already exists on target directory: {}", file.getName(), syncDirectoriesInfo.targetDirPath());
        } catch (IOException e) {
            stopAndFailIfMaxAttemptsReached(e);
        }
    }

    @Override
    public void onFileDelete(File file) {
        try {
            syncDeleted(file.toPath());
        } catch (IOException e) {
            stopAndFailIfMaxAttemptsReached(e);
        }
    }

    private void syncDeleted(Path filePath) throws IOException {
        log.info("Detected file deletion: " + filePath);
        SyncFile syncFile = syncFileFactory.create(filePath);
        try {
            syncFile.delete(syncDirectoriesInfo.targetDirPath());
            log.info("Deleted file: {}", syncFile.getTargetFile(syncDirectoriesInfo.targetDirPath()));
        } catch (FileNotFoundException | NoSuchFileException e) {
            log.warn("File already deleted: {}", syncFile.getTargetFile(syncDirectoriesInfo.targetDirPath()));
        }
    }

    private void syncFileCreated(Path filePath) throws IOException {
        log.info("Detected file creation: '{}'", filePath);
        SyncFile syncFile = syncFileFactory.create(filePath);
        syncFile.copy(syncDirectoriesInfo.targetDirPath());
        log.info("Copied file: {} to: {}", filePath, syncFile.getTargetFile(syncDirectoriesInfo.targetDirPath()));
    }
}