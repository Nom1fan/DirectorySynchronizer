package org.dirsync.model.file;

import java.nio.file.Path;

public interface SyncFileFactory {
    SyncFile create(Path path);
}
