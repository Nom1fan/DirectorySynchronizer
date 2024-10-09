package org.dirsync.controller.event;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.dirsync.controller.event.FileSystemEvent.Type.CREATED;
import static org.dirsync.controller.event.FileSystemEvent.Type.DELETED;

@Slf4j
@UtilityClass
public class FileSystemEventUtils {

    public Set<FileSystemEvent> resolveDuplicates(Set<FileSystemEvent> events) {
        return new HashSet<>(events.stream()
                .collect(Collectors.toMap(
                        FileSystemEvent::path,
                        event -> event,
                        FileSystemEventUtils::resolveConflict
                ))
                .values());
    }

    private FileSystemEvent resolveConflict(FileSystemEvent e1, FileSystemEvent e2) {
        log.warn("Detected duplicate events. Event #1: {}, Event #2: {}", e1, e2);
        if (e1.path().toFile().exists()) {
            FileSystemEvent event = new FileSystemEvent(e1.path(), CREATED);
            log.warn("Resolved conflict by keeping event: {}", event);
            return event;
        }
        FileSystemEvent event = new FileSystemEvent(e1.path(), DELETED);
        log.warn("Resolved conflict by keeping event: {}", event);
        return event;
    }
}
