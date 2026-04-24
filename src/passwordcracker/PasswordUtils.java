package passwordcracker;

/**
 * Utility class providing shared password-generation helpers.
 *
 * <p>
 * The search space is all strings of a given length drawn from
 * {@link #ALPHABET} (the 26 lower-case letters). Every string is
 * uniquely identified by a numeric index in the range
 * {@code [0, 26^length)}, which makes it easy to split work among
 * threads without overlap.
 * </p>
 *
 * <p>
 * Ordering example for length 3:
 * </p>
 * 
 * <pre>
 *   index 0  → "aaa"
 *   index 1  → "aab"
 *   …
 *   index 25 → "aaz"
 *   index 26 → "aba"
 *   …
 * </pre>
 */
public class PasswordUtils {

    /** The character set used for all passwords (a–z). */
    public static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    // Private constructor — this class is a pure utility; no instances needed.
    private PasswordUtils() {
    }

    // ---------------------------------------------------------------
    // Public helpers
    // ---------------------------------------------------------------

    /**
     * Returns the total number of distinct passwords of the given length.
     *
     * <p>
     * Calculated as {@code 26^length}.
     * </p>
     *
     * @param length the password length (must be &gt; 0)
     * @return total number of candidate passwords
     */
    public static long totalPasswords(int length) {
        long total = 1;
        for (int i = 0; i < length; i++) {
            total *= 26; // multiply by alphabet size for each position
        }
        return total;
    }

    /**
     * Converts a numeric index to its corresponding password string.
     *
     * <p>
     * The mapping treats position {@code length - 1} as the least
     * significant "digit" (base 26), mirroring how numbers are written
     * in positional notation.
     * </p>
     *
     * @param index  the password index in {@code [0, totalPasswords(length))}
     * @param length the desired password length
     * @return the password string that corresponds to {@code index}
     */
    public static String indexToPassword(long index, int length) {
        char[] password = new char[length];

        // Fill from the rightmost character to the leftmost
        for (int i = length - 1; i >= 0; i--) {
            password[i] = ALPHABET[(int) (index % 26)]; // current "digit"
            index /= 26; // shift right in base-26
        }

        return new String(password);
    }
}
