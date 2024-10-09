package org.dirsync.controller;

import lombok.extern.slf4j.Slf4j;
import org.dirsync.controller.event.FileSystemEvent;
import org.dirsync.exception.DirectorySyncFailedException;
import org.dirsync.input.SyncDirectoriesValidator;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.dirsync.model.file.SyncFile;
import org.dirsync.model.file.SyncFileFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.dirsync.controller.event.FileSystemEvent.Type.CREATED;
import static org.dirsync.controller.event.FileSystemEvent.Type.DELETED;

@Slf4j
public class DirectorySynchronizerImpl implements DirectorySynchronizer {

    private final SyncDirectoriesInfo syncDirectoriesInfo;
    private final DirectoryWatchService watchService;
    private final SyncFileFactory syncFileFactory;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private boolean failed = false;

    private static final int DIR_SYNC_MAX_NUM_RETIRES = Integer.parseInt(System.getProperty("dir.sync.max.num.retries", "3"));

    private int dirSyncNumRetries = 0;

    public DirectorySynchronizerImpl(SyncDirectoriesInfo syncDirectoriesInfo,
                                     DirectoryWatchService watchService, SyncFileFactory syncFileFactory) {
        this.syncDirectoriesInfo = syncDirectoriesInfo;
        this.watchService = watchService;
        this.syncFileFactory = syncFileFactory;
        SyncDirectoriesValidator.validate(syncDirectoriesInfo);
        registerWatchService();
    }

    private void registerWatchService() {
        log.info("Registering to watch service");
        String sourceDirPath = syncDirectoriesInfo.sourceDirPath();
        watchService.registerRoot(sourceDirPath);
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public Thread syncDirectories() {
        return executeBackgroundThread();
    }

    private Thread executeBackgroundThread() {
        log.info("Starting directory synchronization thread");
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        log.info("Synchronizing directories: " + syncDirectoriesInfo);
        while (keepRunning()) {
            internalSyncDirectories();
        }
        log.info("Directory synchronization stopped");
    }

    private boolean keepRunning() {
        return running.get();
    }

    private void setFailed() {
        failed = true;
    }

    private void internalSyncDirectories() {
        try {
            listenAndSynchronize();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopAndFailIfMaxAttemptsReached(e);
        } catch (FileNotFoundException | NoSuchFileException e) {
            log.warn("File not found: {}", e.getMessage());
        } catch (Exception e) {
            stopAndFailIfMaxAttemptsReached(e);
        }
    }

    private void stopAndFailIfMaxAttemptsReached(Exception e) {
        dirSyncNumRetries++;
        if (dirSyncNumRetries > DIR_SYNC_MAX_NUM_RETIRES) {
            log.error("Directory synchronization failed after {} retries", DIR_SYNC_MAX_NUM_RETIRES, e);
            setFailed();
            stop();
            throw new DirectorySyncFailedException(e);
        }
        log.error("Directory synchronization failed. Recover attempt {}/{}", dirSyncNumRetries, DIR_SYNC_MAX_NUM_RETIRES, e);
    }

    //Visible for testing
    void listenAndSynchronize() throws InterruptedException, IOException {
        Set<FileSystemEvent> fileSystemEvents = watchService.pollEvents();
        log.info("Received file system events: {}", fileSystemEvents);
        for (FileSystemEvent fileSystemEvent : fileSystemEvents) {
            performSync(fileSystemEvent);
        }
    }

    public void stop() {
        running.set(false);
    }

    private void performSync(FileSystemEvent syncEvent) throws IOException {
        Path path = syncEvent.path();
        FileSystemEvent.Type type = syncEvent.type();
        if (type == CREATED) {
            syncCreated(path);
        } else if (type == DELETED) {
            syncDeleted(path);
        }
    }

    private void syncDeleted(Path filePath) throws IOException {
        if (filePath.toFile().isDirectory()) {
            log.warn("Skipping directory deletion (should be handled per file event): '{}'", filePath);
            return;
        }
        log.info("Detected file deletion: " + filePath);
        SyncFile syncFile = syncFileFactory.create(filePath);
        try {
            syncFile.delete(syncDirectoriesInfo.targetDirPath());
            log.info("Deleted file: {}", syncFile.getTargetFile(syncDirectoriesInfo.targetDirPath()));
        } catch (FileNotFoundException e) {
            log.warn("File already deleted: {}", syncFile.getTargetFile(syncDirectoriesInfo.targetDirPath()));
        }
    }

    private void syncCreated(Path filePath) throws IOException {
        if (filePath.toFile().isDirectory()) {
            syncDirectoryCreated(filePath);
            return;
        }
        syncFileCreated(filePath);
    }

    private void syncDirectoryCreated(Path filePath) throws IOException {
        watchService.registerSubDirectory(filePath.toString());
        syncFilesMissedBeforeRegistration(filePath);
    }

    private void syncFilesMissedBeforeRegistration(Path dirPath) throws IOException {
        File[] files = dirPath.toFile().listFiles();
        if (files == null ) {
            return;
        }
        for (File file : files) {
            syncCreated(file.toPath());
        }
    }

    private void syncFileCreated(Path filePath) throws IOException {
        log.info("Detected file creation: '{}'", filePath);
        SyncFile syncFile = syncFileFactory.create(filePath);
        syncFile.copy(syncDirectoriesInfo.targetDirPath());
        log.info("Copied file: {} to: {}", filePath,  syncFile.getTargetFile(syncDirectoriesInfo.targetDirPath()));
    }
}