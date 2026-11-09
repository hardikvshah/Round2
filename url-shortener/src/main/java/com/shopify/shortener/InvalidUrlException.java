package com.shopify.shortener;

/** Thrown when a long URL fails validation. */
public class InvalidUrlException extends IllegalArgumentException {
    public InvalidUrlException(String message) {
        super(message);
    }
}
