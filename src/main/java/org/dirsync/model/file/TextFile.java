package org.dirsync.model.file;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
class TextFile extends DefaultFile {

    TextFile(File file) {
        super(file);
    }

    @Override
    public void delete(String targetDirPath) throws IOException {
        File targetFile = new File(targetDirPath + File.separator + file.getName());
        if (!targetFile.exists()) {
            log.warn("File for deletion: {} not found in target directory: {}", file.getName(), targetDirPath);
            return;
        }
        Files.delete(targetFile.toPath());
    }

    @Override
    public File getTargetFile(String targetDirPath) {
        return new File(targetDirPath + File.separator + file.getName());
    }
}