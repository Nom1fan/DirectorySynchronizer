package org.dirsync.controller.event;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public record FileSystemEvent(Path path, FileSystemEvent.Type type) {

    public enum Type {
        CREATED,
        DELETED
    }
}
