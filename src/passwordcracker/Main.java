package passwordcracker;

/**
 * Password Cracker: Driver Class
 *
 * <p>
 * Runs Part 1 (single-threaded, 3-character password) and
 * Part 2 (multi-threaded, 5-character password) back-to-back.
 * </p>
 *
 * =========================================================
 * TIMING RESULTS
 * =========================================================
 * Part 2: 3 threads : _______ ms
 * Part 2: 4 threads : _______ ms
 * =========================================================
 */
public class Main {

    // -------------------------------------------------------
    // File paths: relative to the project root.
    // Adjust if your working directory differs.
    // -------------------------------------------------------

    /** Path to the 3-character encrypted zip (Part 1). */
    private static final String PROTECTED_3_PATH = "src/passwordcracker/protected3.zip";

    /** Path to the 5-character encrypted zip (Part 2). */
    private static final String PROTECTED_5_PATH = "src/passwordcracker/example.zip";

    /**
     * Application entry point.
     *
     * <p>
     * Runs the single-threaded cracker for Part 1, then the
     * multi-threaded cracker for Part 2. Change
     * {@link MultiThreadedCracker#numThreads} to 3 or 4 to
     * collect the timing data required by the rubric.
     * </p>
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {

        // ── Part 1 ─────────────────────────────────────────
        // Single-threaded brute-force over all 3-char passwords.
        // SingleThreadCracker.crack(PROTECTED_3_PATH, 3);

        // ── Part 2 ─────────────────────────────────────────
        // Multi-threaded brute-force over all 5-char passwords.
        // Toggle numThreads between 3 and 4 to compare timing.
        MultiThreadedCracker.numThreads = 10; // ← change to 3 or 4
        MultiThreadedCracker.crack(PROTECTED_5_PATH, 4);
    }
}
