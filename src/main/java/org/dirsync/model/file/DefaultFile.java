package org.dirsync.model.file;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Slf4j
class DefaultFile implements SyncFile {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private final Supplier<LocalDateTime> localDateTimeSupplier;

    protected final File file;

    DefaultFile(File file) {
        this(file, LocalDateTime::now);
    }

    DefaultFile(File file, Supplier<LocalDateTime> localDateTimeSupplier) {
        this.file = file;
        this.localDateTimeSupplier = localDateTimeSupplier;
    }

    @Override
    public void copy(@NonNull String targetDirPath) throws IOException {
        File targetFile = getTargetFile(targetDirPath);
        FileUtils.copyFile(file, targetFile);
    }

    @Override
    public void delete(String targetDirPath) throws IOException {
        String fileNameRegex = getFileNameRegex();
        File targetFile = findTargetFile(fileNameRegex, targetDirPath);
        if (targetFile == null) {
            log.warn("File for deletion: {} not found in target directory: {} via filename regex: {}",
                    file.getName(), targetDirPath, fileNameRegex);
            return;
        }
        Files.delete(targetFile.toPath());
    }

    private File findTargetFile(String fileNameRegex, String targetDirPath) {
        Pattern pattern = Pattern.compile(fileNameRegex);
        File targetDir = new File(targetDirPath);
        File[] files = targetDir.listFiles();
        if (files == null) {
            return null;
        }
        Optional<File> optionalFile = Arrays.stream(files)
                .filter(f -> pattern.matcher(f.getName()).find())
                .findAny();
        return optionalFile.orElse(null);
    }

    @Override
    public File getTargetFile(String targetDirPath) {
        String baseName = FilenameUtils.getBaseName(file.getName());
        String nameWithTimestamp = baseName + createTimestamp() + "." + FilenameUtils.getExtension(file.getName());
        return new File(targetDirPath + File.separator + nameWithTimestamp);
    }

    private String getFileNameRegex() {
        String baseName = FilenameUtils.getBaseName(file.getName());
        String extension = FilenameUtils.getExtension(file.getName());
        return baseName + "\\[\\d{2}:\\d{2}:\\d{2}\\]." + extension;
    }

    private String createTimestamp() {
        LocalDateTime now = localDateTimeSupplier.get();
        return OPEN_BRACKET + now.format(dateTimeFormatter) + CLOSE_BRACKET;
    }
}
