package com.shopify.shortener;

/**
 * Encodes non-negative counter values as fixed-length (7 char) base62 codes.
 *
 * Alphabet index = digit value, so 'a' is the zero digit. Padding with 'a'
 * (not the literal '0', which is digit 52) keeps the mapping one-to-one.
 */
public final class Base62Codec {

    static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    static final int BASE = ALPHABET.length();   // 62
    public static final int CODE_LENGTH = 7;
    /** 62^7 — number of distinct codes; valid inputs are [0, CAPACITY). */
    public static final long CAPACITY = pow(BASE, CODE_LENGTH);

    private Base62Codec() {
    }

    public static String encode(long n) {
        if (n < 0 || n >= CAPACITY) {
            throw new IllegalArgumentException("value out of range [0, 62^7): " + n);
        }
        char[] buf = new char[CODE_LENGTH];
        for (int i = CODE_LENGTH - 1; i >= 0; i--) {
            buf[i] = ALPHABET.charAt((int) (n % BASE));
            n /= BASE;
        }
        return new String(buf);
    }

    public static long decode(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            throw new IllegalArgumentException("code must be " + CODE_LENGTH + " chars: " + code);
        }
        long n = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = ALPHABET.indexOf(code.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("invalid character in code: " + code);
            }
            n = n * BASE + digit;
        }
        return n;
    }

    private static long pow(long base, int exp) {
        long result = 1;
        for (int i = 0; i < exp; i++) {
            result *= base;
        }
        return result;
    }
}
