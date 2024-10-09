package org.dirsync.controller;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dirsync.controller.event.FileSystemEvent;
import org.dirsync.controller.event.FileSystemEventUtils;
import org.dirsync.exception.DirectoryWatchFailedException;
import org.dirsync.util.RetryUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Slf4j
public class DirectoryWatchServiceImpl implements DirectoryWatchService {

    private final WatchService watchService;
    private String rootDirectory;

    private static final Map<WatchEvent.Kind<?>, FileSystemEvent.Type> kindToEventMap = Map.of(
            ENTRY_CREATE, FileSystemEvent.Type.CREATED,
            ENTRY_DELETE, FileSystemEvent.Type.DELETED
    );

    //ForTesting
    DirectoryWatchServiceImpl() {
        watchService = null;
    }

    public DirectoryWatchServiceImpl(WatchService watchService) {
        this.watchService = watchService;
    }

    @Override
    public void registerRoot(String rootDirectory) throws DirectoryWatchFailedException {
        try {
            this.rootDirectory = rootDirectory;
            register(rootDirectory);
        } catch (Exception ex) {
            throw new DirectoryWatchFailedException("Failed registering root directory: '" + rootDirectory + "' to watch service", ex);
        }
    }

    @Override
    public void registerSubDirectory(String subDirectory) throws DirectoryWatchFailedException {
        try {
            RetryUtils.retryWithInterval(() -> register(subDirectory),
                    "Failed to register sub-directory: '" + subDirectory + "'");
        } catch (Exception ex) {
            throw new DirectoryWatchFailedException("Failed registering sub directory: '" + subDirectory + "' to watch service", ex);
        }
    }

    //VisibleForTesting
    @SneakyThrows
    void register(String directory) {
        log.info("Registering directory to watch: '{}'", directory);
        Path path = Paths.get(directory);
        path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    @Override
    public Set<FileSystemEvent> pollEvents() throws InterruptedException {
        WatchKey watchKey = watchService.take();
        List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
        Set<FileSystemEvent> fileSystemEvents = toFileSystemEvents(watchEvents);
        return FileSystemEventUtils.resolveDuplicates(fileSystemEvents);
    }

    private Set<FileSystemEvent> toFileSystemEvents(List<WatchEvent<?>> watchEvents) {
        return watchEvents.stream()
                .map(this::toFileSystemEvent)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private FileSystemEvent toFileSystemEvent(WatchEvent<?> watchEvent) {
        if (invalidEvent(watchEvent)) {
            return null;
        }
        Path fullPath = toFullPath(watchEvent);
        FileSystemEvent.Type type = kindToEventMap.get(watchEvent.kind());
        return new FileSystemEvent(fullPath, type);
    }

    private boolean invalidEvent(WatchEvent<?> watchEvent) {
        return unfamiliarEventKind(watchEvent.kind());
    }

    private Path toFullPath(WatchEvent<?> watchEvent) {
        return Path.of(rootDirectory + File.separator + ((Path) watchEvent.context()).getFileName());
    }

    private boolean unfamiliarEventKind(WatchEvent.Kind<?> kind) {
        boolean unfamiliarEvent = !kindToEventMap.containsKey(kind);
        if (unfamiliarEvent) {
            log.warn("Unfamiliar event kind: {}", kind);
            return true;
        }
        return false;
    }
}