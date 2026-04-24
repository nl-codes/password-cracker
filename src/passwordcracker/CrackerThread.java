package passwordcracker;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Worker thread for the multi-threaded password cracker (Part 2).
 *
 * <p>
 * Each {@code CrackerThread} is responsible for a non-overlapping
 * slice of the total password search space, identified by a contiguous
 * range of indices {@code [startIndex, endIndex)}. It converts each
 * index to a password string and tests it against its own private copy
 * of the target zip file.
 * </p>
 *
 * <h3>Shared state</h3>
 * <ul>
 * <li>{@link #passwordFound} — volatile flag; any thread sets it to
 * {@code true} when it finds the password, causing all other
 * threads to exit their loops promptly.</li>
 * <li>{@link #foundPassword} — volatile string; holds the correct
 * password once discovered.</li>
 * </ul>
 *
 * <h3>File management</h3>
 * Each thread:
 * <ol>
 * <li>Copies the original zip to a uniquely named file.</li>
 * <li>Extracts into a uniquely named directory.</li>
 * <li>Deletes both when it finishes (whether or not it found the
 * password).</li>
 * </ol>
 */
public class CrackerThread extends Thread {

    // ---------------------------------------------------------------
    // Shared volatile state (visible to ALL threads immediately)
    // ---------------------------------------------------------------

    /**
     * Set to {@code true} by the first thread that finds the password.
     *
     * <p>
     * {@code volatile} guarantees that every thread reads the most
     * recently written value without relying on CPU cache.
     * </p>
     */
    public static volatile boolean passwordFound = false;

    /**
     * Stores the discovered password; {@code null} until found.
     *
     * <p>
     * Also {@code volatile} for the same visibility reason.
     * </p>
     */
    public static volatile String foundPassword = null;

    // ---------------------------------------------------------------
    // Per-thread instance fields
    // ---------------------------------------------------------------

    /** First password index this thread will test (inclusive). */
    private final long startIndex;

    /** One past the last password index this thread will test (exclusive). */
    private final long endIndex;

    /** The number of characters in each password candidate. */
    private final int passwordLength;

    /** Path to the original (shared) encrypted zip file. */
    private final String originalZipPath;

    /** Unique integer ID used to create distinct file names. */
    private final int threadId;

    /** Path to this thread's private copy of the zip file. */
    private final String myZipPath;

    /** Path to this thread's private extraction directory. */
    private final String myContentsDir;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    /**
     * Creates a new {@code CrackerThread}.
     *
     * @param threadId        unique thread identifier (0-based)
     * @param startIndex      first password index to test (inclusive)
     * @param endIndex        one past the last index to test (exclusive)
     * @param passwordLength  the length of every candidate password
     * @param originalZipPath path to the original encrypted zip file
     */
    public CrackerThread(int threadId,
            long startIndex,
            long endIndex,
            int passwordLength,
            String originalZipPath) {
        this.threadId = threadId;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.passwordLength = passwordLength;
        this.originalZipPath = originalZipPath;

        // Build unique names so threads never interfere with each other's files
        this.myZipPath = "protected-copy-" + threadId + ".zip";
        this.myContentsDir = "contents-" + threadId;
    }

    // ---------------------------------------------------------------
    // Static helper
    // ---------------------------------------------------------------

    /**
     * Resets the shared volatile flags before a new cracking session.
     *
     * <p>
     * Must be called by the orchestrator before any threads are
     * started, especially if running more than one session in the same JVM.
     * </p>
     */
    public static void reset() {
        passwordFound = false;
        foundPassword = null;
    }

    // ---------------------------------------------------------------
    // Thread body
    // ---------------------------------------------------------------

    /**
     * Thread entry point.
     *
     * <ol>
     * <li>Copies the zip file to a private path.</li>
     * <li>Iterates over the assigned index range, converting each
     * index to a password and testing it.</li>
     * <li>If the correct password is found, sets the shared volatile
     * fields and exits the loop.</li>
     * <li>Cleans up private files before the thread terminates.</li>
     * </ol>
     */
    @Override
    public void run() {

        System.out.printf("[Thread %d] Starting — range [%d, %d)%n",
                threadId, startIndex, endIndex);

        // ── Step 1: make a private copy of the zip ───────────────
        try {
            Files.copy(Path.of(originalZipPath), Path.of(myZipPath));
        } catch (IOException e) {
            System.err.printf("[Thread %d] ERROR copying zip: %s%n",
                    threadId, e.getMessage());
            return; // cannot proceed without our own copy
        }

        // ── Step 2: try every password in our assigned range ──────
        for (long i = startIndex; i < endIndex; i++) {

            // Another thread already found it — bail out early
            if (passwordFound)
                break;

            // Convert numeric index → password string
            String password = PasswordUtils.indexToPassword(i, passwordLength);

            if (tryPassword(password)) {
                // ── We found the password! ──────────────────────
                passwordFound = true; // signal other threads
                foundPassword = password; // store result
                System.out.printf("[Thread %d] Found password: %s%n",
                        threadId, password);
                break;
            }
        }

        // ── Step 3: clean up private files ───────────────────────
        cleanUp();

        System.out.printf("[Thread %d] Done.%n", threadId);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Tests a single candidate password against this thread's zip copy.
     *
     * <p>
     * zip4j throws {@link ZipException} when the password is wrong,
     * so a clean extraction means the password is correct.
     * </p>
     *
     * @param password the candidate to test
     * @return {@code true} if the password opened the zip, {@code false} otherwise
     */
    private boolean tryPassword(String password) {
        try {
            ZipFile zipFile = new ZipFile(myZipPath);
            zipFile.setPassword(password);

            // Throws ZipException if the password is incorrect
            zipFile.extractAll(myContentsDir);

            return true; // extraction succeeded → correct password

        } catch (ZipException e) {
            return false; // wrong password — keep searching
        }
    }

    /**
     * Deletes this thread's private zip copy and extraction directory.
     *
     * <p>
     * Errors are printed but do not cause the thread to throw,
     * since cleanup is a best-effort operation.
     * </p>
     */
    private void cleanUp() {

        // Delete the private zip copy
        try {
            Files.deleteIfExists(Path.of(myZipPath));
        } catch (IOException e) {
            System.err.printf("[Thread %d] Could not delete %s: %s%n",
                    threadId, myZipPath, e.getMessage());
        }

        // Recursively delete the extraction directory
        deleteDirectory(new File(myContentsDir));
    }

    /**
     * Recursively deletes a directory and all files it contains.
     *
     * <p>
     * A directory must be empty before {@link File#delete()} will
     * succeed, so this method processes children first (post-order).
     * </p>
     *
     * @param dir the root of the directory tree to delete
     */
    private void deleteDirectory(File dir) {
        if (!dir.exists())
            return;

        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectory(child); // recurse first
                } else {
                    child.delete(); // delete file
                }
            }
        }
        dir.delete(); // now the directory itself is empty
    }
}
