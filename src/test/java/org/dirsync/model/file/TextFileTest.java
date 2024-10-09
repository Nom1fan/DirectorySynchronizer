package org.dirsync.model.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class TextFileTest {

    private static final String TARGET_DIR_PATH = System.getProperty("java.io.tmpdir") + "/targetDir";

    @Test
    void testSyncHappyPath() throws IOException {
        File txtSourceFile = createTxtSourceFile();
        TextFile textFile = new TextFile(txtSourceFile);
        textFile.copy(TARGET_DIR_PATH);
        File expectedTargetFile = createExpectedTargetFile(txtSourceFile);
        Assertions.assertTrue(expectedTargetFile.exists());
    }

    private File createExpectedTargetFile(File txtSourceFile) {
        return new File(TARGET_DIR_PATH + File.separator + txtSourceFile.getName());
    }

    private File createTxtSourceFile() throws IOException {
        return File.createTempFile("test", ".txt");
    }
}