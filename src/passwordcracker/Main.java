package passwordcracker;

/**
 * Password Cracker: Driver Class
 *
 * <p>
 * Entry point of the program.
 * It calls {@link Orchestrator} to crack passwords of two different password
 * protect zip files.
 * </p>
 *
 * =========================================================
 * TIMING RESULTS
 * =========================================================
 *
 * Part 1: protected3.zip (3-char password)
 * 1 thread -> search: 1136 ms
 *
 * Part 2: protected5.zip (5-char password)
 * 3 threads -> search: 982780 ms
 * 4 threads -> search: 178723 ms
 *
 * =========================================================
 */
public class Main {

    // -------------------------------------------------------
    // File paths: relative to the project root.
    // -------------------------------------------------------

    /**
     * Path to the encrypted zip protected by a 3-character password
     * (Part 1).
     */
    private static final String PROTECTED_3_PATH = "src/passwordcracker/protected3.zip";

    /**
     * Path to the encrypted zip protected by a 5-character password
     * (Part 2).
     */
    private static final String PROTECTED_5_PATH = "src/passwordcracker/protected5.zip";

    /**
     * Application entry point.
     *
     * <p>
     * Runs the cracker twice:
     * once single-threaded (1 thread) for the 3-character zip, and
     * once multi-threaded (3 or 4 threads) for the 5-character zip.
     * </p>
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {

        /*
         * Passwords for Both protected3.zip and protected5.zip are cracked in the same
         * run.
         * Please comment of one of the Orchestrator.crack() to crack them one by one.
         */

        // Number of threads to use to crack passwrods
        int numThreads = 1;

        // ── Part 1: 3-character password, 1 thread ────────────────
        Orchestrator.crack(PROTECTED_3_PATH, 3, numThreads);

        // Output to separate Single Threaded and Multi threaded Outputs
        System.out.println("* ".repeat(25) + "\n");

        // ── Part 2: 5-character password, multiple threads ─────────
        // Change numThreads to >1 to use multi-threading
        numThreads = 4;
        Orchestrator.crack(PROTECTED_5_PATH, 5, numThreads);
    }
}
