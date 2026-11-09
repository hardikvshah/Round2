package com.shopify.shortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlShortenerTest {

    private static final String OWN_HOST = "shop.ify";
    private UrlShortener shortener;

    @BeforeEach
    void setUp() {
        shortener = new UrlShortener(OWN_HOST);
    }

    // --- happy path ---

    @Test
    void roundTrip() {
        String url = "https://example.com/products/42?ref=email";
        String code = shortener.shorten(url);
        assertEquals(Optional.of(url), shortener.expand(code));
    }

    @Test
    void codeIsSevenBase62Characters() {
        String code = shortener.shorten("https://example.com");
        assertTrue(code.matches("[a-zA-Z0-9]{7}"), code);
    }

    @Test
    void sameUrlTwiceGetsDifferentCodes() {
        String a = shortener.shorten("https://example.com");
        String b = shortener.shorten("https://example.com");
        assertNotEquals(a, b);
        assertEquals(shortener.expand(a), shortener.expand(b));
    }

    @Test
    void whitespaceIsTrimmed() {
        String code = shortener.shorten("   https://example.com/x  ");
        assertEquals(Optional.of("https://example.com/x"), shortener.expand(code));
    }

    // --- expand misses ---

    @Test
    void unknownCodeIsEmpty() {
        assertEquals(Optional.empty(), shortener.expand("zzzzzzz"));
    }

    @Test
    void nullCodeIsEmpty() {
        assertEquals(Optional.empty(), shortener.expand(null));
    }

    @Test
    void codesAreCaseSensitive() {
        String code = shortener.shorten("https://example.com"); // "aaaaaaa"
        String flipped = code.toUpperCase();
        assertNotEquals(code, flipped);
        assertEquals(Optional.empty(), shortener.expand(flipped));
    }

    // --- validation ---

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not a url", "example.com", "ftp://example.com/file",
            "javascript:alert(1)", "https://", "http://exa mple.com"})
    void invalidUrlsAreRejected(String bad) {
        assertThrows(InvalidUrlException.class, () -> shortener.shorten(bad));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://shop.ify/aaaaaab", "http://SHOP.IFY/x", "https://www.shop.ify/x"})
    void urlsPointingBackToShortenerAreRejected(String loop) {
        assertThrows(InvalidUrlException.class, () -> shortener.shorten(loop));
    }

    @Test
    void lookalikeHostIsAllowed() {
        // "notshop.ify" is a different domain, must not be caught by the loop check.
        String code = shortener.shorten("https://notshop.ify/x");
        assertEquals(Optional.of("https://notshop.ify/x"), shortener.expand(code));
    }

    // --- uniqueness & capacity ---

    @Test
    void manyCodesAreUnique() {
        int n = 10_000;
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < n; i++) {
            codes.add(shortener.shorten("https://example.com/" + i));
        }
        assertEquals(n, codes.size());
    }

    @Test
    void exhaustedCodeSpaceFailsLoudly() {
        UrlShortener nearlyFull = new UrlShortener(OWN_HOST, Base62Codec.CAPACITY - 1);
        assertEquals("9999999", nearlyFull.shorten("https://example.com/last"));
        assertThrows(IllegalStateException.class, () -> nearlyFull.shorten("https://example.com/one-more"));
    }
}
