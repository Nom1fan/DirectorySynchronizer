package org.dirsync.controller;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.dirsync.exception.DirectoryWatchFailedException;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.dirsync.model.file.SyncFile;
import org.dirsync.model.file.SyncFileFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DirectorySynchronizerImplTest {

    @Mock
    private SyncDirectoriesInfo syncDirectoriesInfo;

    @Mock
    private FileAlterationMonitor fileAlterationMonitor;

    @Mock
    private SyncFileFactory syncFileFactory;

    @Mock
    private SyncFile syncFile;

    private DirectorySynchronizerImpl directorySynchronizer;

    private AutoCloseable mocks;

    private static final String SOURCE_DIR = "sourceDir";

    private static final String TARGET_DIR = "targetDir";

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    @BeforeEach
    void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        File sourceDir = new File(TMP_DIR, SOURCE_DIR);
        File targetDir = new File(TMP_DIR, TARGET_DIR);
        if (!sourceDir.exists()) {
            Files.createDirectory(sourceDir.toPath());
        }
        if (!targetDir.exists()) {
            Files.createDirectory(targetDir.toPath());
        }
        when(syncDirectoriesInfo.sourceDirPath()).thenReturn(sourceDir.getAbsolutePath());
        when(syncDirectoriesInfo.targetDirPath()).thenReturn(targetDir.getAbsolutePath());
        directorySynchronizer = new DirectorySynchronizerImpl(syncDirectoriesInfo, fileAlterationMonitor, syncFileFactory);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void testStart() throws Exception {
        directorySynchronizer.start();
        assertTrue(directorySynchronizer.isRunning());
        verify(fileAlterationMonitor).start();
    }

    @Test
    void testStartFailure() throws Exception {
        doThrow(new Exception("Monitor start failure")).when(fileAlterationMonitor).start();
        assertThrows(DirectoryWatchFailedException.class, () -> directorySynchronizer.start());
        assertFalse(directorySynchronizer.isRunning());
    }

    @Test
    void testStop() throws Exception {
        directorySynchronizer.start();
        directorySynchronizer.stop();
        assertFalse(directorySynchronizer.isRunning());
        verify(fileAlterationMonitor).stop();
    }

    @Test
    void testOnFileCreate() throws IOException {
        File file = new File("newFile.txt");
        when(syncFileFactory.create(file.toPath())).thenReturn(syncFile);

        directorySynchronizer.onFileCreate(file);

        verify(syncFile).copy(syncDirectoriesInfo.targetDirPath());
    }

    @Test
    void testOnFileDelete() throws IOException {
        File file = new File("deletedFile.txt");
        when(syncFileFactory.create(file.toPath())).thenReturn(syncFile);

        directorySynchronizer.onFileDelete(file);

        verify(syncFile).delete(syncDirectoriesInfo.targetDirPath());
    }

    @Test
    void testOnFileDeleteFileNotFound() throws IOException {
        File file = new File("deletedFile.txt");
        when(syncFileFactory.create(file.toPath())).thenReturn(syncFile);
        doThrow(new IOException("File not found")).when(syncFile).delete(anyString());

        directorySynchronizer.onFileDelete(file);

        verify(syncFile).delete(syncDirectoriesInfo.targetDirPath());
    }
}