package org.dirsync.view;

import org.apache.commons.io.IOUtils;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsoleViewTest {
    private static final String CHOICE_CANCEL_AND_EXIT = "0";
    private static final String SOURCE_DIR_PATH = System.getProperty("java.io.tmpdir") + "sourceSyncDir/";
    private static final String TARGET_DIR_PATH = System.getProperty("java.io.tmpdir") + "targetSyncDir/";

    @BeforeAll
    static void beforeAll() {
        new File(SOURCE_DIR_PATH).mkdirs();
        new File(TARGET_DIR_PATH).mkdirs();
    }

    @Test
    void happyPath() {
        InputStream choices = prepareHappyPathChoices();
        ConsoleView consoleView = new ConsoleView(choices);
        SyncDirectoriesInfo syncDirectoriesInfo = consoleView.runMenuLoop();
        assertNotNull(syncDirectoriesInfo);
        assertEquals(SOURCE_DIR_PATH, syncDirectoriesInfo.sourceDirPath());
        assertEquals(TARGET_DIR_PATH, syncDirectoriesInfo.targetDirPath());
    }

    @Test
    void testExit() {
        InputStream choices = prepareExit();
        ConsoleView consoleView = new ConsoleView(choices);
        SyncDirectoriesInfo syncDirectoriesInfo = consoleView.runMenuLoop();
        assertNull(syncDirectoriesInfo);
    }

    @Test
    void testInvalidChoice() {
        InputStream choices = prepareInvalidChoice();
        ConsoleView consoleView = new ConsoleView(choices);
        SyncDirectoriesInfo syncDirectoriesInfo = consoleView.runMenuLoop();
        assertNull(syncDirectoriesInfo);
    }

    @Test
    void testSelectSourceAndCancel() {
        InputStream choices = prepareSelectSourceAndCancel();
        ConsoleView consoleView = new ConsoleView(choices);
        SyncDirectoriesInfo syncDirectoriesInfo = consoleView.runMenuLoop();
        assertNull(syncDirectoriesInfo);
    }

    private static InputStream prepareHappyPathChoices() {
        return IOUtils.toInputStream(String.join("\n", SOURCE_DIR_PATH, TARGET_DIR_PATH), UTF_8);
    }

    private InputStream prepareSelectSourceAndCancel() {
        return IOUtils.toInputStream(String.join("\n", SOURCE_DIR_PATH, CHOICE_CANCEL_AND_EXIT), UTF_8);
    }

    private InputStream prepareInvalidChoice() {
        return IOUtils.toInputStream(String.join("\n", "invalid choice", CHOICE_CANCEL_AND_EXIT),
                UTF_8);
    }

    private InputStream prepareExit() {
        return IOUtils.toInputStream(String.join("\n", CHOICE_CANCEL_AND_EXIT), UTF_8);
    }
}