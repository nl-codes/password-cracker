package passwordcracker;

/**
 * Unified password cracker that works with any number of threads.
 *
 * <p>
 * Pass {@code numThreads = 1} for single-threaded behaviour (Part 1),
 * or any integer &gt; 1 for multi-threaded behaviour (Part 2).
 * The search logic is identical in both cases: only the number of concurrent
 * workers changes.
 * </p>
 *
 * <h3>Timing</h3>
 * <p>
 * <b>Search time</b>: time taken by thread to find correct password.
 * </p>
 *
 * <h3>Work distribution</h3>
 * <p>
 * The {@code 26^passwordLength} candidates are indexed {@code [0, total)}.
 * Thread {@code i} owns the contiguous slice {@code [i*chunk, (i+1)*chunk)};
 * the last thread absorbs any remainder so no candidate password is skipped.
 * </p>
 *
 * <pre>
 * // Single-threaded (Part 1)
 * Orchestrator.crack("protected3.zip", 3, 1);
 *
 * // Multi-threaded (Part 2)
 * Orchestrator.crack("protected5.zip", 5, >1);
 * </pre>
 */
public class Orchestrator {

    /**
     * Attempts to crack the password of an encrypted zip file.
     *
     * @param zipPath        path to the encrypted {@code .zip} file
     * @param passwordLength exact length of the password (letters only)
     * @param numThreads     number of worker threads to use;
     *                       {@code 1} gives single-threaded behaviour
     * @throws IllegalArgumentException if {@code numThreads} &lt; 1
     */
    public static void crack(String zipPath, int passwordLength, int numThreads) {

        if (numThreads < 1) {
            throw new IllegalArgumentException("numThreads must be at least 1, got: " + numThreads);
        }

        boolean isSingleThreaded = numThreads == 1;

        // ── Banner ────────────────────────────────────────────────
        System.out.println("=".repeat(50));
        System.out.printf(" Password Cracker: %s%n",
                isSingleThreaded ? "Single-threaded" : "Multi-threaded: (" + numThreads + " threads)");
        System.out.println("=".repeat(50));
        System.out.printf("  Target file    : %s%n", zipPath);
        System.out.printf("  Password length: %d character(s)%n", passwordLength);
        System.out.printf("  Thread count   : %d%n", numThreads);
        System.out.println("-".repeat(50));

        // ── Phase 1: Setup / password-space generation ────────────

        // Reset shared volatile state from any previous call
        CrackerThread.reset();

        // Total candidates: 26^passwordLength
        long totalPasswords = PasswordUtils.totalPasswords(passwordLength);

        // Base slice size per thread (integer division)
        long chunkSize = totalPasswords / numThreads;

        // Build thread objects and assign each its index range
        CrackerThread[] threads = new CrackerThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            long startIndex = (long) i * chunkSize;

            // Last thread absorbs any remainder from integer division
            long endIndex = (i == numThreads - 1) ? totalPasswords
                    : (long) (i + 1) * chunkSize;

            threads[i] = new CrackerThread(i, startIndex, endIndex,
                    passwordLength, zipPath);
        }

        System.out.printf("  Total candidates        : %,d%n", totalPasswords);
        System.out.printf("  Candidates per thread   : ~%,d%n", chunkSize);
        System.out.println("-".repeat(50));
        System.out.println("  Searching…");

        // ── Phase 2: Launch threads and wait ─────────────────────
        // Timing starts here: captures actual search time (zip I/O).
        long searchStart = System.currentTimeMillis();

        for (CrackerThread thread : threads) {
            thread.start();
        }

        // Block until every thread has finished and cleaned up
        for (CrackerThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
        }

        long searchEnd = System.currentTimeMillis();
        long searchMs = searchEnd - searchStart;

        // ── Report ────────────────────────────────────────────────
        System.out.println("-".repeat(50));
        if (CrackerThread.foundPassword != null) {
            System.out.printf("  Password found          : %s%n",
                    CrackerThread.foundPassword);
        } else {
            System.out.println("  Password NOT found in search space.");
        }
        System.out.printf("  Search time             : %d ms%n", searchMs);
        System.out.println("=".repeat(50));
        System.out.println();
    }
}
