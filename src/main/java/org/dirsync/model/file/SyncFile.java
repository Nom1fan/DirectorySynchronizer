package org.dirsync.model.file;

import lombok.NonNull;

import java.io.File;
import java.io.IOException;

public interface SyncFile {

    void copy(@NonNull String targetDirPath) throws IOException;

    void delete(String targetDirPath) throws IOException;

    File getTargetFile(String targetDirPath);
}
