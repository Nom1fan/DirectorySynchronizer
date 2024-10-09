package org.dirsync.input;

import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

class SyncDirectoriesInfoValidatorTest {

    private static final String SOURCE_DIR_PATH = System.getProperty("java.io.tmpdir") + "sourceSyncDir/";
    private static final String TARGET_DIR_PATH = System.getProperty("java.io.tmpdir") + "targetSyncDir/";

    private static Stream<Arguments> nullAndEmptySyncDirs() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of(SOURCE_DIR_PATH, null),
                Arguments.of(null, TARGET_DIR_PATH)
        );
    }

    @BeforeEach
    void beforeEach() {
        deleteSyncDirectories();
    }

    private static void deleteSyncDirectories() {
        File sourceDir = new File(SOURCE_DIR_PATH);
        if (sourceDir.exists()) {
            Assertions.assertTrue(sourceDir.delete());
        }
        File targetDir = new File(TARGET_DIR_PATH);
        if (targetDir.exists()) {
            Assertions.assertTrue(targetDir.delete());
        }
    }

    @Test
    void testValidateHappyPath() {
        createSourceAndTargetDirs();
        SyncDirectoriesValidator.validate(new SyncDirectoriesInfo(SOURCE_DIR_PATH, TARGET_DIR_PATH));
    }

    private static void createSourceAndTargetDirs() {
        createSourceDir();
        createTargetDir();
    }

    private static void createTargetDir() {
        boolean targetDirCreated = new File(TARGET_DIR_PATH).mkdirs();
        Assertions.assertTrue(targetDirCreated, "Target directory was not created");
    }

    private static void createSourceDir() {
        boolean sourceDirCreated = new File(SOURCE_DIR_PATH).mkdirs();
        Assertions.assertTrue(sourceDirCreated, "Source directory was not created");
    }

    @ParameterizedTest
    @MethodSource("nullAndEmptySyncDirs")
    void testNullAndEmptySyncDirs(String sourceDirPath, String targetDirPath) {
        if (StringUtils.isNotBlank(sourceDirPath)) {
            createSourceDir();
        }
        SyncDirectoriesInfo syncDirectoriesInfo = new SyncDirectoriesInfo(sourceDirPath, targetDirPath);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SyncDirectoriesValidator.validate(syncDirectoriesInfo));
        Assertions.assertEquals("Directory path cannot be null", exception.getMessage());
    }

    @Test
    void testSourcePathIsNotDirectory() throws IOException {
        String sourceFilePath = File.createTempFile("sourceFile", ".txt").getAbsolutePath();
        SyncDirectoriesInfo syncDirectoriesInfo = new SyncDirectoriesInfo(sourceFilePath, TARGET_DIR_PATH);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SyncDirectoriesValidator.validate(syncDirectoriesInfo));
        Assertions.assertEquals("Path '" + sourceFilePath + "' is not a directory", exception.getMessage());
    }

    @Test
    void testTargetPathIsNotDirectory() throws IOException {
        createSourceDir();
        String targetFilePath = File.createTempFile("targetFile", ".txt").getAbsolutePath();
        SyncDirectoriesInfo syncDirectoriesInfo = new SyncDirectoriesInfo(SOURCE_DIR_PATH, targetFilePath);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SyncDirectoriesValidator.validate(syncDirectoriesInfo));
        Assertions.assertEquals("Path '" + targetFilePath + "' is not a directory", exception.getMessage());

    }

    @Test
    void testSourceAndTargetPathsAreTheSame() {
        createSourceDir();
        SyncDirectoriesInfo syncDirectoriesInfo = new SyncDirectoriesInfo(SOURCE_DIR_PATH, SOURCE_DIR_PATH);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> SyncDirectoriesValidator.validate(syncDirectoriesInfo));
        Assertions.assertEquals("Source and target directories cannot be the same", exception.getMessage());
    }

}