package org.dirsync;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.dirsync.controller.DirectorySynchronizerImpl;
import org.dirsync.controller.DirectorySynchronizer;
import org.dirsync.model.dir.SyncDirectoriesInfo;
import org.dirsync.model.file.SyncFileFactoryImpl;
import org.dirsync.view.ConsoleView;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ConsoleView consoleView = new ConsoleView(System.in);
        SyncDirectoriesInfo syncDirectoriesInfo = consoleView.runMenuLoop();
        if (syncDirectoriesInfo == null) {
            log.info("Exiting.");
            System.exit(0);
        }
        FileAlterationMonitor fileAlterationMonitor = createFileAlterationMonitor();
        DirectorySynchronizer directorySynchronizer =
                createDirectorySynchronizer(syncDirectoriesInfo, fileAlterationMonitor);
        try {
            directorySynchronizer.start();
            consoleView.runSyncLoop();
            System.exit(0);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Application failure. Exiting.", e);
            System.exit(1);
        }
    }

    private static DirectorySynchronizer createDirectorySynchronizer(SyncDirectoriesInfo syncDirectoriesInfo,
                                                                     FileAlterationMonitor fileAlterationMonitor) {
        SyncFileFactoryImpl syncFileFactory = new SyncFileFactoryImpl();
        return new DirectorySynchronizerImpl(syncDirectoriesInfo, fileAlterationMonitor, syncFileFactory);
    }

    private static FileAlterationMonitor createFileAlterationMonitor() {
        return new FileAlterationMonitor(500);
    }
}