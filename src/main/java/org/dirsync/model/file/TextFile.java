package org.dirsync.model.file;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
class TextFile extends DefaultFile {

    TextFile(File file) {
        super(file);
    }

    @Override
    public File getTargetFile(String targetDirPath) {
        return new File(targetDirPath + File.separator + file.getName());
    }
}
