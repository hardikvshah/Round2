package com.shopify.shortener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62CodecTest {

    @Test
    void zeroEncodesToAllZeroDigits() {
        assertEquals("aaaaaaa", Base62Codec.encode(0));
    }

    @Test
    void lastSingleDigit() {
        assertEquals("aaaaaa9", Base62Codec.encode(61));
    }

    @Test
    void firstCarry() {
        assertEquals("aaaaaba", Base62Codec.encode(62));
    }

    @Test
    void largestValue() {
        assertEquals("9999999", Base62Codec.encode(Base62Codec.CAPACITY - 1));
    }

    @Test
    void capacityIsRejectedNotTruncated() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.encode(Base62Codec.CAPACITY));
    }

    @Test
    void negativeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.encode(-1));
    }

    @Test
    void paddingDoesNotCollideWithZeroCharacter() {
        // The bug we discussed: padding with '0' (digit 52) would make these equal.
        long big = Base62Codec.decode("000000b");
        assertNotEquals(Base62Codec.encode(1), Base62Codec.encode(big));
    }

    @Test
    void decodeIsInverseOfEncode() {
        long[] samples = {0, 1, 61, 62, 125, 3_844, 56_800_235_584L, Base62Codec.CAPACITY - 1};
        for (long n : samples) {
            assertEquals(n, Base62Codec.decode(Base62Codec.encode(n)));
        }
    }
}
