package org.dirsync.model.file;

import org.apache.commons.io.FilenameUtils;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class SyncFileFactoryImpl implements SyncFileFactory {

    @Override
    public SyncFile create(Path path) {
        File file = path.toFile();
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        return switch (extension) {
            case "txt" -> new TextFile(file);
            default -> new DefaultFile(file, LocalDateTime::now);
        };
    }
}
