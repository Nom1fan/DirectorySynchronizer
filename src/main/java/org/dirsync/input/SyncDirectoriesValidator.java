package org.dirsync.input;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.dirsync.model.dir.SyncDirectoriesInfo;

import java.io.File;

@UtilityClass
public class SyncDirectoriesValidator {

    public void validate(SyncDirectoriesInfo syncDirectoriesInfo) {
        String sourceDirPath = syncDirectoriesInfo.sourceDirPath();
        String targetDirPath = syncDirectoriesInfo.targetDirPath();
        validate(sourceDirPath, targetDirPath);
    }

    public void validate(String sourceDirPath, String targetDirPath) {
        validateDirectory(sourceDirPath);
        validateDirectory(targetDirPath);

        if (sourceDirPath.equals(targetDirPath)) {
            throw new IllegalArgumentException("Source and target directories cannot be the same");
        }
    }

    public void validateDirectory(String dirPath) {
        if (StringUtils.isBlank(dirPath)) {
            throw new IllegalArgumentException("Directory path cannot be null");
        }
        if (!new File(dirPath).isDirectory()) {
            throw new IllegalArgumentException(String.format("Path '%s' is not a directory", dirPath));
        }
    }
}
