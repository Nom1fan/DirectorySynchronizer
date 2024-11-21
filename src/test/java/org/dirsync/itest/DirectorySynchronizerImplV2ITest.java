package org.dirsync.itest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.awaitility.Awaitility;
import org.dirsync.controller.DirectorySynchronizerImpl;
import org.dirsync.controller.DirectorySynchronizer;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.dirsync.model.file.SyncFileFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class DirectorySynchronizerImplV2ITest {

    private static final String SOURCE_DIR_PATH = System.getProperty("java.io.tmpdir") + "sourceDir";
    private static final String TARGET_DIR_PATH = System.getProperty("java.io.tmpdir") + "targetDir";
    private static final File SOURCE_DIR = new File(SOURCE_DIR_PATH);
    private static final File TARGET_DIR = new File(TARGET_DIR_PATH);
    private DirectorySynchronizer directorySynchronizerV2;

    @BeforeEach
    void beforeEach() throws IOException, InterruptedException {
        resetDirs();
        FileAlterationMonitor fileAlterationMonitor = createFileAlterationMonitor();
        SyncDirectoriesInfo syncDirectoriesInfo = createSyncDirectoriesInfo();
        directorySynchronizerV2 = new DirectorySynchronizerImpl(syncDirectoriesInfo, fileAlterationMonitor, new SyncFileFactoryImpl());
        directorySynchronizerV2.start();
    }

    private static FileAlterationMonitor createFileAlterationMonitor() {
        return new FileAlterationMonitor(200);
    }

    private static SyncDirectoriesInfo createSyncDirectoriesInfo() {
        return new SyncDirectoriesInfo(SOURCE_DIR_PATH, TARGET_DIR_PATH);
    }

    @Test
    void happyPathCreateTxtFile() throws IOException {
        waitUntilRunning(directorySynchronizerV2);
        String newFileName = "newFile.txt";
        createNewFileInSourceDir(newFileName);
        waitUntilTxtFileCopied(newFileName);
    }

    @Test
    void happyPathCreateTxtFileAlreadyExistsInTarget() throws IOException {
        String newFileName = "newFile.txt";
        createNewFileInTargetDir(newFileName);
        waitUntilRunning(directorySynchronizerV2);
        createNewFileInSourceDir(newFileName);
        assertTrue(directorySynchronizerV2.isRunning());
        assertFalse(directorySynchronizerV2.isFailed());
    }

    @Test
    void happyPathCreateTxtFileSubFolder() throws IOException {
        waitUntilRunning(directorySynchronizerV2);
        String newFileName = "newFile.txt";
        createNewFileInSourceDirSubFolder("subfolder", newFileName);
        waitUntilTxtFileCopied(newFileName);
    }

    @Test
    void happyPathCreateTxtFile2ndDepthSubFolder() throws IOException {
        waitUntilRunning(directorySynchronizerV2);
        String newFileName = "newFile.txt";
        createNewFileInSourceDirSubFolder("subfolder1/subfolder2", newFileName);
        waitUntilTxtFileCopied(newFileName);
    }

    @Test
    void happyPathDeleteTxtFile() throws IOException, InterruptedException {
        String newFileName = "newFile.txt";
        createNewFileInSourceDir(newFileName);
        createNewFileInTargetDir(newFileName);

        directorySynchronizerV2 = new DirectorySynchronizerImpl(createSyncDirectoriesInfo(), createFileAlterationMonitor(), new SyncFileFactoryImpl());
        directorySynchronizerV2.start();
        waitUntilRunning(directorySynchronizerV2);

        deleteFileFromSourceDir(newFileName);
        waitUntilTxtFileDeletedFromTargetDir(newFileName);
    }

    @Test
    void happyPathDeleteTxtFileSubFolder() throws IOException, InterruptedException {
        String newFileName = "newFile.txt";
        createNewFileInSourceDirSubFolder("subfolder", newFileName);
        createNewFileInTargetDir(newFileName);

        directorySynchronizerV2 = new DirectorySynchronizerImpl(createSyncDirectoriesInfo(), createFileAlterationMonitor(), new SyncFileFactoryImpl());
        directorySynchronizerV2.start();
        waitUntilRunning(directorySynchronizerV2);

        String filePathInSourceDir = "subfolder/" + newFileName;
        deleteFileFromSourceDir(filePathInSourceDir);
        waitUntilTxtFileDeletedFromTargetDir(newFileName);
    }

    @Test
    void happyPathDeleteTxtFile2ndDepthSubFolder() throws IOException, InterruptedException {
        String newFileName = "newFile.txt";
        createNewFileInSourceDirSubFolder("subfolder1/subfolder2", newFileName);
        createNewFileInTargetDir(newFileName);

        directorySynchronizerV2 = new DirectorySynchronizerImpl(createSyncDirectoriesInfo(), createFileAlterationMonitor(), new SyncFileFactoryImpl());
        directorySynchronizerV2.start();
        waitUntilRunning(directorySynchronizerV2);

        String filePathInSourceDir = "subfolder1/subfolder2/" + newFileName;
        deleteFileFromSourceDir(filePathInSourceDir);
        waitUntilTxtFileDeletedFromTargetDir(newFileName);
    }

    @Test
    void happyPathCreateAndDeleteTxtFile() throws IOException {
        waitUntilRunning(directorySynchronizerV2);
        String newFileName = "newFile.txt";
        createNewFileInSourceDir(newFileName);
        waitUntilTxtFileCopied(newFileName);
        deleteFileFromSourceDir(newFileName);
        waitUntilTxtFileDeletedFromTargetDir(newFileName);
    }

    @Test
    void happyPathRenameTxtFile() throws IOException {
        waitUntilRunning(directorySynchronizerV2);
        String newFileName = "newFile.txt";
        createNewFileInSourceDir(newFileName);
        waitUntilTxtFileCopied(newFileName);
        String renamedFileName = "renamedFile.txt";
        renameFileInSourceDir(newFileName, renamedFileName);
        waitUntilTxtFileDeletedFromTargetDir(newFileName);
        waitUntilTxtFileCopied(renamedFileName);
    }

    @Test
    void happyPathCreateGeneralFile() throws IOException {
        waitUntilRunning(directorySynchronizerV2);
        String sourceFilename = "newFile.xxx";
        String targetFilenameRegex = getFilenameRegex(sourceFilename);
        createNewFileInSourceDir(sourceFilename);
        waitUntilGeneralFileCopied(targetFilenameRegex);
    }

    private String getFilenameRegex(String filename) {
        String baseName = FilenameUtils.getBaseName(filename);
        String extension = FilenameUtils.getExtension(filename);
        return baseName + "\\[\\d{2}:\\d{2}:\\d{2}\\]." + extension;
    }

    @Test
    void happyPathDeleteGeneralFile() throws IOException, InterruptedException {
        String sourceFilename = "newFile.xxx";
        String targetFilename = "newFile[11:11:11].xxx";
        createNewFileInSourceDir(sourceFilename);
        createNewFileInTargetDir(targetFilename);

        directorySynchronizerV2 = new DirectorySynchronizerImpl(createSyncDirectoriesInfo(), createFileAlterationMonitor(), new SyncFileFactoryImpl());
        directorySynchronizerV2.start();
        waitUntilRunning(directorySynchronizerV2);

        deleteFileFromSourceDir(sourceFilename);
        waitUntilGeneralFileDeleted(targetFilename);
    }

    private static void renameFileInSourceDir(String newFileName, String renamedFileName) {
        File newFile = new File(SOURCE_DIR_PATH + "/" + newFileName);
        File renamedFile = new File(SOURCE_DIR_PATH + "/" + renamedFileName);
        assertTrue(newFile.renameTo(renamedFile));
    }

    private static void deleteFileFromSourceDir(String fileName) throws IOException {
        File newFile = new File(SOURCE_DIR_PATH + "/" + fileName);
        log.info("Deleting file: " + newFile);
        FileUtils.delete(newFile);
    }

    private static void resetDirs() throws IOException {
        FileUtils.deleteDirectory(SOURCE_DIR);
        FileUtils.deleteDirectory(TARGET_DIR);
        assertTrue(SOURCE_DIR.mkdirs(), "Failed to create source directory");
        assertTrue(TARGET_DIR.mkdirs(), "Failed to create target directory");
    }

    private static void createNewFileInSourceDir(String newFileName) throws IOException {
        assertTrue(new File(SOURCE_DIR_PATH + "/" + newFileName).createNewFile());
    }

    private static void createNewFileInSourceDirSubFolder(String subFolder, String newFileName) throws IOException {
        assertTrue(new File(SOURCE_DIR_PATH + "/" + subFolder).mkdirs());
        String newFileSubPath = subFolder + "/" + newFileName;
        createNewFileInSourceDir(newFileSubPath);
    }

    private static void createNewFileInTargetDir(String newFileName) throws IOException {
        assertTrue(new File(TARGET_DIR_PATH + "/" + newFileName).createNewFile());
    }

    private void waitUntilRunning(DirectorySynchronizer directorySynchronizerV2) {
        Awaitility.await()
                .alias("Expected DirectorySynchronizerV2 to be running but it is not")
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(directorySynchronizerV2.isRunning()));
    }

    private static void waitUntilTxtFileCopied(String newFileName) {
        Awaitility.await()
                .alias("Expected :'" + newFileName + "' to be synced to: '" + TARGET_DIR_PATH + "' but it does not exist")
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    File newFile = new File(TARGET_DIR_PATH + "/" + newFileName);
                    assertTrue(newFile.exists());
                });
    }

    private void waitUntilTxtFileDeletedFromTargetDir(String fileName) {
        Awaitility.await()
                .alias("Expected : '" + fileName + "' to be deleted from: '" + TARGET_DIR_PATH + "' but it exists")
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    File file = new File(TARGET_DIR_PATH + "/" + fileName);
                    assertFalse(file.exists());
                });
    }

    private static void waitUntilGeneralFileCopied(String filenameRegex) {
        Awaitility.await()
                .alias("Expected :'" + filenameRegex + "' to be synced to: '" + TARGET_DIR_PATH + "' but it does not exist")
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean found = findTargetFile(filenameRegex);
                    assertTrue(found);
                });
    }

    private static void waitUntilGeneralFileDeleted(String newFileName) {
        String newFileBaseName = FilenameUtils.getBaseName(newFileName);
        Awaitility.await()
                .alias("Expected :'" + newFileName + "' to be synced to: '" + TARGET_DIR_PATH + "' but it does not exist")
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean found = findTargetFile(newFileBaseName);
                    assertFalse(found);
                });
    }

    private static boolean findTargetFile(String fileNameRegex) {
        Pattern pattern = Pattern.compile(fileNameRegex);
        File targetDir = new File(TARGET_DIR_PATH);
        File[] files = targetDir.listFiles();
        if (files == null) {
            return false;
        }
        return Arrays.stream(files)
                .anyMatch(f -> pattern.matcher(f.getName()).find());
    }
}