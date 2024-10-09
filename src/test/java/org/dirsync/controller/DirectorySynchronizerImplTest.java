package org.dirsync.controller;

import org.dirsync.controller.event.FileSystemEvent;
import org.dirsync.exception.DirectorySyncFailedException;
import org.dirsync.exception.DirectoryWatchFailedException;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.dirsync.model.file.SyncFile;
import org.dirsync.model.file.SyncFileFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.dirsync.controller.event.FileSystemEvent.Type.CREATED;
import static org.dirsync.controller.event.FileSystemEvent.Type.DELETED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DirectorySynchronizerImplTest {

    @Mock
    private SyncDirectoriesInfo syncDirectoriesInfo;
    @Mock
    private DirectoryWatchService watchService;
    @Mock
    private SyncFileFactory syncFileFactory;
    private DirectorySynchronizerImpl directorySynchronizer;
    private static final String SOURCE_DIR_PATH = System.getProperty("java.io.tmpdir") + "/sourceDir";
    private static final String TARGET_DIR_PATH = System.getProperty("java.io.tmpdir") + "/targetDir";

    private AutoCloseable mocks;

    @BeforeAll
    static void beforeAll() {
        new File(SOURCE_DIR_PATH).mkdirs();
        new File(TARGET_DIR_PATH).mkdirs();
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        givenValidSyncDirectories();
        directorySynchronizer = new DirectorySynchronizerImpl(syncDirectoriesInfo, watchService, syncFileFactory);
    }

    private void givenValidSyncDirectories() {
        when(syncDirectoriesInfo.sourceDirPath()).thenReturn(SOURCE_DIR_PATH);
        when(syncDirectoriesInfo.targetDirPath()).thenReturn(TARGET_DIR_PATH);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void testConstructor_RegistersWatchServiceAndValidatesDirectories() throws DirectoryWatchFailedException {
        DirectoryWatchService mockWatchService = mock(DirectoryWatchService.class);
        directorySynchronizer = new DirectorySynchronizerImpl(syncDirectoriesInfo, mockWatchService, syncFileFactory);
        verify(mockWatchService).registerRoot(SOURCE_DIR_PATH);
    }

    @ParameterizedTest
    @MethodSource("invalidDirectories")
    void testConstructor_ThrowsExceptionForInvalidDirectories(String sourceDir, String targetDir) {
        when(syncDirectoriesInfo.sourceDirPath()).thenReturn(sourceDir);
        when(syncDirectoriesInfo.targetDirPath()).thenReturn(targetDir);
        assertThrows(IllegalArgumentException.class,
                () -> new DirectorySynchronizerImpl(syncDirectoriesInfo, watchService, syncFileFactory));
    }

    public static Stream<Arguments> invalidDirectories() {
        return Stream.of(
                Arguments.of(null, TARGET_DIR_PATH),
                Arguments.of(SOURCE_DIR_PATH, null),
                Arguments.of("", TARGET_DIR_PATH),
                Arguments.of(SOURCE_DIR_PATH, ""),
                Arguments.of("invalid", TARGET_DIR_PATH),
                Arguments.of(SOURCE_DIR_PATH, "invalid"),
                Arguments.of(SOURCE_DIR_PATH, SOURCE_DIR_PATH)
        );
    }

    @Test
    void testRun_EmptyEvents() throws Exception {
        givenWatchServiceReturnNoEventsAndStopSync();
        directorySynchronizer.run();
        verify(watchService, atLeastOnce()).pollEvents();
    }

    @Test
    void testRun_CreateEvent() throws Exception {
        givenWatchServiceReturnEventsAndStopSync(CREATED);
        SyncFile mockSyncFile = givenSyncFileFactoryCreatesSyncFile();
        directorySynchronizer.run();
        verify(mockSyncFile).copy(anyString());
    }

    @Test
    void testRun_DeleteEvent() throws Exception {
        givenWatchServiceReturnEventsAndStopSync(DELETED);
        SyncFile mockSyncFile = givenSyncFileFactoryCreatesSyncFile();
        directorySynchronizer.run();
        verify(mockSyncFile).delete(anyString());
    }

    private SyncFile givenSyncFileFactoryCreatesSyncFile() {
        SyncFile mockSyncFile = mock(SyncFile.class);
        when(syncFileFactory.create(any(Path.class))).thenReturn(mockSyncFile);
        return mockSyncFile;
    }

    private void givenWatchServiceReturnNoEventsAndStopSync() throws InterruptedException {
        when(watchService.pollEvents()).thenAnswer((Answer<Set<FileSystemEvent>>) invocationOnMock -> {
            directorySynchronizer.stop();
            return Set.of();
        });
    }

    private void givenWatchServiceReturnEventsAndStopSync(FileSystemEvent.Type type) throws InterruptedException {
        givenWatchServiceReturnEventsAndStopSync(type, mock(Path.class));
    }

    private void givenWatchServiceReturnEventsAndStopSync(FileSystemEvent.Type type, Path path) throws InterruptedException {
        when(watchService.pollEvents()).thenAnswer((Answer<Set<FileSystemEvent>>) invocationOnMock -> {
            directorySynchronizer.stop();
            return Set.of(new FileSystemEvent(path, type));
        });
    }

    @Test
    void testRun_WatchServiceThrowsInterruptedExceptionFailAfterMaxAttempts() throws Exception {
        givenWatchServiceThrowsInterruptedException();
        DirectorySyncFailedException exception = assertThrows(DirectorySyncFailedException.class,
                () -> directorySynchronizer.run());
        Assertions.assertEquals("Directory synchronization failed.", exception.getMessage());
        Assertions.assertTrue(directorySynchronizer.isFailed());
        Assertions.assertFalse(directorySynchronizer.isRunning());
    }

    private void givenWatchServiceThrowsInterruptedException() throws InterruptedException {
        when(watchService.pollEvents()).thenThrow(new InterruptedException());
    }

    @ParameterizedTest
    @ValueSource(classes = {InterruptedException.class, RuntimeException.class})
    @Timeout(5)
    void testRun_FailAndStopAfterMaxRetries(Class<Exception> thrownException) throws Exception {
        givenWatchServiceThrowsException(thrownException);
        DirectorySyncFailedException exception = assertThrows(DirectorySyncFailedException.class,
                () -> directorySynchronizer.run());
        Assertions.assertEquals("Directory synchronization failed.", exception.getMessage());
    }

    private void givenWatchServiceThrowsException(Class<Exception> thrownException) throws InterruptedException {
        when(watchService.pollEvents()).thenThrow(thrownException);
    }

    @Test
    void testStop_SetsRunningToFalse() {
        directorySynchronizer.stop();
        assertFalse(directorySynchronizer.isRunning());
    }
}
