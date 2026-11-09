# URL Shortener (Stage 1)

In-memory URL shortener in Java 17. `shorten(longUrl)` returns a 7-character base62 code; `expand(code)` returns `Optional<String>` with the original URL.

## Requirements

- JDK 17+
- Maven 3.8+
- JUnit 5.10 (pulled by Maven from Maven Central)

## Build and test

```bash
mvn test            # compile + run all unit tests
mvn package         # build target/url-shortener-1.0-SNAPSHOT.jar
```

## Usage

```java
UrlShortener s = new UrlShortener("shop.ify");          // the shortener's own host
String code = s.shorten("https://example.com/p/42");     // "aaaaaaa"
Optional<String> url = s.expand(code);                   // Optional[https://example.com/p/42]
s.expand("zzzzzzz");                                     // Optional.empty → HTTP layer maps to 404
```

## Layout

```
src/main/java/com/shopify/shortener/
  Base62Codec.java          counter <-> 7-char code
  UrlShortener.java         shorten / expand, URL validation
  InvalidUrlException.java  thrown for bad input
src/test/java/com/shopify/shortener/
  Base62CodecTest.java      encoding boundaries
  UrlShortenerTest.java     API behaviour, validation, uniqueness, capacity
```
