package org.dirsync.view;

import org.dirsync.input.SyncDirectoriesValidator;
import org.dirsync.model.dir.SyncDirectoriesInfo;

import java.io.InputStream;
import java.util.Scanner;

public class ConsoleView {

    private final Scanner scanner;

    public ConsoleView(InputStream in) {
        scanner = new Scanner(in);
    }

    public SyncDirectoriesInfo runMenuLoop() {
        System.out.println("Welcome to Directory Synchronizer!");
        System.out.println("This program will synchronize two directories.");
        System.out.println("You will be prompted to enter the source and target directories.");
        System.out.println("Press 0 to exit");
        boolean continueLoop = true;
        SyncDirectoriesInfo syncDirectoriesInfo = null;
        while (continueLoop) {
            String sourceDirPath = readInputAndValidate("Enter source directory:");
            if (sourceDirPath == null) {
                return null;
            }
            String targetDirPath = readInputAndValidate("Enter target directory:");
            if (targetDirPath == null) {
                return null;
            }
            if (validate(sourceDirPath, targetDirPath)) {
                syncDirectoriesInfo = new SyncDirectoriesInfo(sourceDirPath, targetDirPath);
                continueLoop = false;
            }
        }
        return syncDirectoriesInfo;
    }

    private static boolean validate(String sourceDirPath, String targetDirPath) {
        try {
            SyncDirectoriesValidator.validate(sourceDirPath, targetDirPath);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void runSyncLoop() {
        System.out.println("Listening for synchronization events...");
        System.out.println("Press any key to exit.");
        scanner.next();
        System.out.println("Exiting. Bye!");
    }

    private String readInputAndValidate(String message) {
        boolean continueInputLoop = true;
        String input = null;
        while (continueInputLoop) {
            System.out.println(message);
            String line = scanner.nextLine().trim();
            if ("0".equals(line)) {
                System.out.println("Exiting.");
                continueInputLoop = false;
            } else if (validate(line)) {
                input = line;
                continueInputLoop = false;
            }
        }
        return input;
    }

    private boolean validate(String dir) {
        try {
            SyncDirectoriesValidator.validateDirectory(dir);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
