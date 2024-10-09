package org.dirsync.model.file;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Supplier;

class DefaultFileTest {

    private static final int EXPECTED_HOUR = 22;
    private static final int EXPECTED_MINUTE = 12;
    private static final int EXPECTED_SECOND = 50;
    private static final String TARGET_DIR_PATH = System.getProperty("java.io.tmpdir") + "/target";

    @Test
    void testSyncHappyPath() throws IOException {
        File nonTxtSourceFile = createNonTxtSourceFile();
        DefaultFile defaultFile = new DefaultFile(nonTxtSourceFile, createConstantTimeSupplierForFilenameTimestamp());
        defaultFile.copy(TARGET_DIR_PATH);
        File expectedTargetFile = createExpectedTargetFile(nonTxtSourceFile);
        Assertions.assertTrue(expectedTargetFile.exists());
    }

    @Test
    void testDeleteHappyPath() throws IOException {
        File nonTxtSourceFile = createNonTxtSourceFile();
        File expectedTargetFile = createExpectedTargetFile(nonTxtSourceFile);
        Assertions.assertTrue(expectedTargetFile.createNewFile(), "Failed to create target file");
        DefaultFile defaultFile = new DefaultFile(nonTxtSourceFile, createConstantTimeSupplierForFilenameTimestamp());
        defaultFile.delete(TARGET_DIR_PATH);
        Assertions.assertFalse(expectedTargetFile.exists());
    }

    private static File createNonTxtSourceFile() throws IOException {
        String nonTxtExtension = FileTestUtils.generateRandomStringExcludingTxt();
        return File.createTempFile("test", "." + nonTxtExtension);
    }

    private static Supplier<LocalDateTime> createConstantTimeSupplierForFilenameTimestamp() {
        return () -> LocalDateTime.of(2021, 1, 1, EXPECTED_HOUR, EXPECTED_MINUTE, EXPECTED_SECOND);
    }

    private static File createExpectedTargetFile(File srcFile) {
        return new File(TARGET_DIR_PATH + File.separator +
                FilenameUtils.getBaseName(srcFile.getName()) + "[" + EXPECTED_HOUR + ":" + EXPECTED_MINUTE + ":" + EXPECTED_SECOND + "]." +
                FilenameUtils.getExtension(srcFile.getName()));
    }
}