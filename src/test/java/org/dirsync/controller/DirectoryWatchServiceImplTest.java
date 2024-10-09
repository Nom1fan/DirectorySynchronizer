package org.dirsync.controller;

import org.dirsync.controller.event.FileSystemEvent;
import org.dirsync.exception.DirectoryWatchFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DirectoryWatchServiceImplTest {

    @Mock
    private WatchService watchService;

    private DirectoryWatchServiceImpl directoryWatchService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        directoryWatchService = new DirectoryWatchServiceImpl(watchService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void registerWatchSuccessfully() throws Exception {
        final String directory = "directory";
        DirectoryWatchServiceImpl spyDirectoryWatchService = spy(DirectoryWatchServiceImpl.class);
        doNothing().when(spyDirectoryWatchService).register(directory);
        spyDirectoryWatchService.registerRoot(directory);
        verify(spyDirectoryWatchService).register(directory);
    }

    @Test
    void registerWatchThrowsException() throws IOException {
        final String directory = "directory";
        DirectoryWatchServiceImpl spyDirectoryWatchService = spy(DirectoryWatchServiceImpl.class);
        doThrow(new RuntimeException()).when(spyDirectoryWatchService).register(directory);
        DirectoryWatchFailedException exception = assertThrows(DirectoryWatchFailedException.class, () ->
                spyDirectoryWatchService.registerRoot(directory));
        Assertions.assertEquals("Failed registering to watch service", exception.getMessage());
    }

    @Test
    void pollEmptyEvents() throws Exception {
        givenWatchServiceReturnsNoEvents();
        assertDoesNotThrow(() -> directoryWatchService.pollEvents());
    }

    private void givenWatchServiceReturnsNoEvents() throws InterruptedException {
        givenWatchServiceReturnsEvents(List.of());
    }

    private void givenWatchServiceReturnsEvents(List<WatchEvent<?>> events) throws InterruptedException {
        WatchKey watchKey = mock(WatchKey.class);
        when(watchService.take()).thenReturn(watchKey);
        when(watchKey.pollEvents()).thenReturn(events);
    }

    @Test
    void pollEventsThrowsInterruptedException() throws InterruptedException {
        when(watchService.take()).thenThrow(InterruptedException.class);
        assertThrows(InterruptedException.class, () -> directoryWatchService.pollEvents());
    }

    @Test
    void pollValidEvents() throws InterruptedException {
        givenWatchServiceReturnsValidEvents();

        Set<FileSystemEvent> fileSystemEvents = directoryWatchService.pollEvents();
        Assertions.assertNotNull(fileSystemEvents);
        Assertions.assertFalse(fileSystemEvents.isEmpty());

        boolean foundCreated = eventsContainType(fileSystemEvents, FileSystemEvent.Type.CREATED);
        boolean foundDeleted = eventsContainType(fileSystemEvents, FileSystemEvent.Type.DELETED);

        Assertions.assertTrue(foundCreated, "Expected CREATED event but was not found");
        Assertions.assertTrue(foundDeleted, "Expected DELETED event but was not found");
    }

    @Test
    void pollInvalidEvents() throws InterruptedException {
        givenWatchServiceReturnsInvalidEvents();
        Set<FileSystemEvent> fileSystemEvents = directoryWatchService.pollEvents();
        Assertions.assertNotNull(fileSystemEvents);
        Assertions.assertTrue(fileSystemEvents.isEmpty());
    }

    private void givenWatchServiceReturnsInvalidEvents() throws InterruptedException {
        WatchKey watchKey = mock(WatchKey.class);
        when(watchService.take()).thenReturn(watchKey);
        WatchEvent<?> invalidEvent1 = createWatchEvent(mock(Path.class), StandardWatchEventKinds.OVERFLOW);
        WatchEvent<?> invalidEvent2 = createWatchEvent(mock(Path.class), StandardWatchEventKinds.ENTRY_MODIFY);
        when(watchKey.pollEvents()).thenReturn(List.of(invalidEvent1, invalidEvent2));
    }

    private static boolean eventsContainType(Set<FileSystemEvent> fileSystemEvents, FileSystemEvent.Type type) {
        return fileSystemEvents.stream().anyMatch(fileSystemEvent -> fileSystemEvent.type() == type);
    }

    private void givenWatchServiceReturnsValidEvents() throws InterruptedException {
        WatchEvent<?> watchEventCreate = createWatchEvent(mock(Path.class), StandardWatchEventKinds.ENTRY_CREATE);
        WatchEvent<?> watchEventDelete = createWatchEvent(mock(Path.class), StandardWatchEventKinds.ENTRY_DELETE);
        givenWatchServiceReturnsEvents(List.of(watchEventCreate, watchEventDelete));
    }

    private static WatchEvent<?> createWatchEvent(Path path, WatchEvent.Kind<?> kind) {
        WatchEvent watchEvent = mock(WatchEvent.class);
        when(watchEvent.context()).thenReturn(path);
        when(watchEvent.kind()).thenReturn(kind);
        return watchEvent;
    }
}