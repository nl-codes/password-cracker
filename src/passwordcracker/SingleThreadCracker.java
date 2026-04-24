package passwordcracker;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;

/**
 * Single-threaded brute-force password cracker (Part 1).
 *
 * <p>Generates every possible password of the specified length using
 * recursion, testing each against the target zip file until the correct
 * password is found.  Intended for 3-character passwords.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   SingleThreadCracker.crack("src/passwordcracker/protected3.zip", 3);
 * </pre>
 */
public class SingleThreadCracker {

    // ---------------------------------------------------------------
    // State (reset before each crack attempt)
    // ---------------------------------------------------------------

    /** Set to true the moment the correct password is discovered. */
    private static boolean found = false;

    /** Holds the cracked password once found; null until then. */
    private static String crackedPassword = null;

    /** Temporary directory used to extract zip contents during testing. */
    private static final String CONTENTS_DIR = "contents-single";

    // Private constructor — static-only class.
    private SingleThreadCracker() {}

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Attempts to crack the password of an encrypted zip file.
     *
     * <p>Tries every combination of lower-case letters of exactly
     * {@code passwordLength} characters.  Prints the password and the
     * elapsed time when successful.</p>
     *
     * @param zipPath        path to the encrypted {@code .zip} file
     * @param passwordLength exact length of the password to search for
     */
    public static void crack(String zipPath, int passwordLength) {

        System.out.println("=== Part 1: Single-Threaded Cracker ===");
        System.out.printf("Target file   : %s%n", zipPath);
        System.out.printf("Password length: %d character(s)%n", passwordLength);
        System.out.println("Searching…");

        // Reset shared state so the method can be called more than once
        found = false;
        crackedPassword = null;

        long startTime = System.currentTimeMillis();

        // Kick off the recursive search with an empty prefix
        generateAndTry("", passwordLength, zipPath);

        long elapsed = System.currentTimeMillis() - startTime;

        // Report result
        if (crackedPassword != null) {
            System.out.printf("Password found : %s%n", crackedPassword);
            System.out.printf("Time taken     : %d ms%n%n", elapsed);
        } else {
            System.out.println("Password not found in the search space.");
        }
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Recursively builds candidate passwords character-by-character and
     * tests each complete password against the zip file.
     *
     * <p>Base case: {@code remaining == 0} — {@code prefix} is a full
     * password; test it immediately.</p>
     *
     * <p>Recursive case: append each letter of the alphabet to
     * {@code prefix} and recurse with {@code remaining - 1}.</p>
     *
     * @param prefix    the portion of the password built so far
     * @param remaining how many more characters still need to be appended
     * @param zipPath   path to the protected zip file
     */
    private static void generateAndTry(String prefix, int remaining, String zipPath) {

        // Base case — we have a complete password; test it
        if (remaining == 0) {
            if (!found) {               // skip further attempts once found
                tryPassword(prefix, zipPath);
            }
            return;
        }

        // Recursive case — append every letter and descend
        for (char c : PasswordUtils.ALPHABET) {
            if (found) return;          // short-circuit: password already found
            generateAndTry(prefix + c, remaining - 1, zipPath);
        }
    }

    /**
     * Tests a single password against the zip file.
     *
     * <p>zip4j throws a {@link ZipException} for wrong passwords, so a
     * successful extraction (no exception) means we found the right one.</p>
     *
     * @param password the candidate password to test
     * @param zipPath  path to the protected zip file
     */
    private static void tryPassword(String password, String zipPath) {
        try {
            ZipFile zipFile = new ZipFile(zipPath);
            zipFile.setPassword(password);

            // extractAll throws ZipException if the password is wrong
            zipFile.extractAll(CONTENTS_DIR);

            // ── Correct password ──────────────────────────────────
            found = true;
            crackedPassword = password;

            // Clean up the extracted contents immediately
            deleteDirectory(new File(CONTENTS_DIR));

        } catch (ZipException e) {
            // Wrong password — do nothing and continue the search
        }
    }

    /**
     * Recursively deletes a directory and everything inside it.
     *
     * @param dir the directory (or file) to remove
     */
    private static void deleteDirectory(File dir) {
        if (!dir.exists()) return;

        // Delete contents before the directory itself
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectory(child); // recurse into sub-directories
                } else {
                    child.delete();         // delete individual files
                }
            }
        }
        dir.delete(); // now safe to remove the (empty) directory
    }
}
