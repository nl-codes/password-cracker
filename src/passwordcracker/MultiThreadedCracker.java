package passwordcracker;

/**
 * Multi-threaded password cracker orchestrator (Part 2).
 *
 * <p>
 * Divides the full password search space into non-overlapping
 * contiguous ranges and assigns one range to each {@link CrackerThread}.
 * Threads run concurrently; all threads exit as soon as any one of them
 * discovers the correct password (via the shared volatile flag in
 * {@link CrackerThread#passwordFound}).
 * </p>
 *
 * <h3>How the work is divided</h3>
 * <p>
 * For a 5-character lower-case password there are 26<sup>5</sup>&nbsp;=&nbsp;
 * 11,881,376 candidates. With 4 threads each gets roughly 2.97 million
 * candidates to try. The last thread absorbs any remainder from integer
 * division.
 * </p>
 *
 * <pre>
 *   Thread 0: indices [0,            chunkSize)
 *   Thread 1: indices [chunkSize,  2*chunkSize)
 *   Thread 2: indices [2*chunkSize, 3*chunkSize)
 *   Thread 3: indices [3*chunkSize, totalPasswords)   ← absorbs remainder
 * </pre>
 *
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * MultiThreadedCracker.numThreads = 4;
 * MultiThreadedCracker.crack("src/passwordcracker/protected5.zip", 5);
 * </pre>
 */
public class MultiThreadedCracker {

    /**
     * Number of worker threads to spawn.
     *
     * <p>
     * This is the hard-coded variable required by the project spec.
     * Change it to 3 or 4 (or any positive integer) to experiment with
     * different levels of parallelism.
     * </p>
     */
    public static int numThreads = 4;

    // Private constructor — static-only class.
    private MultiThreadedCracker() {
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Cracks the password of the given encrypted zip file using
     * {@link #numThreads} concurrent worker threads.
     *
     * <p>
     * Timing begins immediately before the threads are started and
     * ends after all threads have finished (via {@link Thread#join()}).
     * This captures both the password-generation work and the file I/O
     * performed by the threads.
     * </p>
     *
     * @param zipPath        path to the encrypted {@code .zip} file
     * @param passwordLength exact number of characters in the password
     */
    public static void crack(String zipPath, int passwordLength) {

        System.out.println("=== Part 2: Multi-Threaded Cracker ===");
        System.out.printf("Target file    : %s%n", zipPath);
        System.out.printf("Password length: %d character(s)%n", passwordLength);
        System.out.printf("Thread count   : %d%n", numThreads);
        System.out.println("Searching…");

        // Reset shared volatile state from any previous run
        CrackerThread.reset();

        // Total number of candidate passwords in the search space
        long totalPasswords = PasswordUtils.totalPasswords(passwordLength);

        // Base number of candidates each thread will handle
        long chunkSize = totalPasswords / numThreads;

        // ── Create threads ─────────────────────────────────────────
        CrackerThread[] threads = new CrackerThread[numThreads];

        // Start timing BEFORE spawning threads (includes all overhead)
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {

            long startIndex = (long) i * chunkSize;

            // The last thread takes any leftover indices so nothing is missed
            long endIndex = (i == numThreads - 1) ? totalPasswords
                    : (long) (i + 1) * chunkSize;

            threads[i] = new CrackerThread(i, startIndex, endIndex,
                    passwordLength, zipPath);
            threads[i].start();
        }

        // ── Wait for all threads to finish ─────────────────────────
        // Even after the password is found the remaining threads will
        // exit naturally (they check passwordFound each iteration) and
        // clean up their files before calling join() can return.
        for (CrackerThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // Restore the interrupted status and move on
                Thread.currentThread().interrupt();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // ── Report result ──────────────────────────────────────────
        if (CrackerThread.foundPassword != null) {
            System.out.printf("Password found : %s%n", CrackerThread.foundPassword);
            System.out.printf("Time taken     : %d ms  (with %d thread(s))%n%n",
                    elapsed, numThreads);
        } else {
            System.out.println("Password not found in the search space.");
        }
    }
}
