package com.shopify.shortener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory URL shortener (Stage 1).
 *
 * - Codes come from a monotonically increasing counter, base62-encoded to 7 chars.
 * - Same long URL shortened twice gets two different codes (by design).
 * - Not thread-safe: Stage 1 assumes single-threaded calls.
 */
public class UrlShortener {

    private final Map<String, String> codeToUrl = new HashMap<>();
    private final String ownHost;
    private long nextId;

    public UrlShortener(String ownHost) {
        this(ownHost, 0L);
    }

    /** Package-private: lets tests start the counter near the end of the code space. */
    UrlShortener(String ownHost, long startId) {
        if (ownHost == null || ownHost.isBlank()) {
            throw new IllegalArgumentException("ownHost is required");
        }
        if (startId < 0 || startId > Base62Codec.CAPACITY) {
            throw new IllegalArgumentException("startId out of range: " + startId);
        }
        this.ownHost = ownHost.toLowerCase(Locale.ROOT);
        this.nextId = startId;
    }

    /** Returns a new 7-char short code for the given URL. */
    public String shorten(String longUrl) {
        String url = validate(longUrl);
        while (true) {
            if (nextId >= Base62Codec.CAPACITY) {
                throw new IllegalStateException("code space exhausted");
            }
            String code = Base62Codec.encode(nextId++);
            // Defensive: the counter alone guarantees uniqueness, but this guards
            // against a counter reset or future custom aliases sharing the map.
            if (codeToUrl.putIfAbsent(code, url) == null) {
                return code;
            }
        }
    }

    /** Returns the original URL, or empty if the code is unknown. */
    public Optional<String> expand(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(codeToUrl.get(code));
    }

    private String validate(String longUrl) {
        if (longUrl == null) {
            throw new InvalidUrlException("url is null");
        }
        String trimmed = longUrl.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidUrlException("url is empty");
        }
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("malformed url: " + trimmed);
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("only http/https allowed: " + trimmed);
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new InvalidUrlException("url has no host: " + trimmed);
        }
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals(ownHost) || h.endsWith("." + ownHost)) {
            throw new InvalidUrlException("url points back to the shortener: " + trimmed);
        }
        return trimmed;
    }
}
