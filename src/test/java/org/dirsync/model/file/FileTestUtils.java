package org.dirsync.model.file;

import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class FileTestUtils {

    public String generateRandomStringExcludingTxt() {
        Random random = new Random();
        StringBuilder sb;

        do {
            sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                char randomChar = (char) (random.nextInt(26) + 'a'); // Random letter between 'a' and 'z'
                sb.append(randomChar);
            }
        } while (sb.toString().equals("txt")); // Exclude "txt"

        return sb.toString();
    }
}
