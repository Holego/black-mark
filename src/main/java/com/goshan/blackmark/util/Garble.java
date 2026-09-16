package com.goshan.blackmark.util;

import java.util.Random;

/**
 * Misremembering, not corruption. Letters are swapped for other letters of the same script,
 * so a name reads like something you almost know.
 * <p>
 * The result is deterministic for a given input, strength and seed bucket, so a tooltip does not
 * shimmer sixty times a second - it only slips when the bucket rolls over.
 */
public final class Garble {

    private static final String LOWER_LATIN = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER_LATIN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CYRILLIC = "абвгдежзик"
            + "лмнопрстухцчшыья";
    private static final String UPPER_CYRILLIC = "АБВГДЕЖЗИК"
            + "ЛМНОПРСТУХЦЧШЫЬЯ";
    private static final String DIGITS = "0123456789";

    /**
     * @param input     the text as it really is
     * @param chance    0..1 probability that any one character slips
     * @param seedBucket changes rarely (a slow tick counter), so the same text garbles the same way for a while
     */
    public static String text(String input, double chance, long seedBucket) {
        if (input == null || input.isEmpty() || chance <= 0.0D) {
            return input;
        }

        Random random = new Random(input.hashCode() * 31L + seedBucket);
        StringBuilder out = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (random.nextDouble() >= chance) {
                out.append(c);
                continue;
            }

            String alphabet = alphabetOf(c);
            if (alphabet == null) {
                out.append(c);
            } else {
                out.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
        }
        return out.toString();
    }

    private static String alphabetOf(char c) {
        if (LOWER_LATIN.indexOf(c) >= 0) {
            return LOWER_LATIN;
        }
        if (UPPER_LATIN.indexOf(c) >= 0) {
            return UPPER_LATIN;
        }
        if (LOWER_CYRILLIC.indexOf(c) >= 0) {
            return LOWER_CYRILLIC;
        }
        if (UPPER_CYRILLIC.indexOf(c) >= 0) {
            return UPPER_CYRILLIC;
        }
        if (DIGITS.indexOf(c) >= 0) {
            return DIGITS;
        }
        return null;
    }

    private Garble() {
    }
}
