# Design — URL Shortener, Stage 1

## Requirements (as clarified)

- Single global namespace; no merchant concept yet (may come in a later stage).
- In memory, single process, single-threaded calls.
- Load: ~100 creates/s peak, ~10k expands/s peak (read-heavy, 100:1). Size the code space for ~1B URLs over the lifetime.
- Codes never expire (for now).
- Same long URL shortened twice → two different codes (allows per-campaign tracking later; no reverse map needed).
- Guessable / sequential codes are acceptable for Stage 1.

## Design

**Code format:** 7 characters of base62 (`a–z A–Z 0–9`), case-sensitive. 62^7 ≈ 3.5 trillion codes, which is enough headroom for 1B URLs.

**Generation:** a monotonically increasing `long` counter, base62-encoded. Alphabet index = digit value, so `a` is zero. Codes are left-padded with `a` (the zero digit), **not** the literal `'0'` (digit 52). Padding with `'0'` would collide: `encode(1)` padded → `000000b`, which is also the natural encoding of 3,002,032,123,273.

**Storage:** `HashMap<String code, String url>`. `expand` is a single O(1) lookup (hot path). `shorten` uses `putIfAbsent` as a defensive uniqueness check. The counter alone guarantees uniqueness, but the check guards against a counter reset or future custom aliases.

**Validation (in `shorten`):**
- null / blank → `InvalidUrlException`
- trims whitespace
- must parse as a URI with scheme `http` or `https` and a non-empty host
- host must not be the shortener's own host or a subdomain of it (prevents redirect loops); look-alikes such as `notshop.ify` are allowed

**Errors:** `expand` returns `Optional.empty()` for unknown/null codes, because a miss is an expected outcome; a future HTTP layer maps it to 404. Exhausting the code space throws `IllegalStateException` instead of silently wrapping.

## Invariants

1. Every issued code is exactly 7 chars from the base62 alphabet.
2. Distinct counter values → distinct codes (`encode` is injective on [0, 62^7)).
3. A code, once issued, always expands to the same URL.
4. `encode(n)` for n outside [0, 62^7) throws; it never truncates.
5. No stored URL points at the shortener's own host.

## Test cases

| Area | Case |
|---|---|
| Encoding | `encode(0)=aaaaaaa`, `encode(61)=aaaaaa9`, `encode(62)=aaaaaba`, `encode(62^7−1)=9999999` |
| Encoding | `encode(62^7)` and `encode(-1)` throw |
| Encoding | padding does not collide with `'0'`; `decode(encode(n)) == n` |
| Happy path | round trip; code matches `[a-zA-Z0-9]{7}`; whitespace trimmed |
| Design choice | same URL twice → different codes, both expand correctly |
| Misses | unknown code → empty; null code → empty; case-flipped code → empty |
| Validation | null, empty, blank, no scheme, `ftp:`, `javascript:`, `https://`, spaces in host → `InvalidUrlException` |
| Loops | `shop.ify`, `SHOP.IFY`, `www.shop.ify` rejected; `notshop.ify` allowed |
| Uniqueness | 10,000 shortens → 10,000 distinct codes |
| Capacity | counter at 62^7−1 issues `9999999`; next call throws `IllegalStateException` |

## Deferred (named, not built)

- Domain blocklist (plug in as a `Predicate<URI>` policy)
- Reserved/offensive words (a counter can generate `Shopify`, which is 7 chars)
- Non-guessable codes (scramble the counter with a reversible mapping before encoding)
- Thread safety (`ConcurrentHashMap` + `AtomicLong`), persistence, multi-merchant namespaces
