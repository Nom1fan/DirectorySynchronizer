package org.dirsync.model.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncFileFactoryTest {

    private final SyncFileFactoryImpl syncFileFactory = new SyncFileFactoryImpl();

    @Test
    void createTxtFile() {
        SyncFile syncFile = syncFileFactory.create(Path.of("filename.txt"));
        assertTrue(syncFile instanceof TextFile);
    }

    @Test
    void createDefaultFile() {
        SyncFile syncFile = syncFileFactory.create(Path.of("filename." + FileTestUtils.generateRandomStringExcludingTxt()));
        assertTrue(syncFile instanceof DefaultFile);
    }
}