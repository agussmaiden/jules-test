package com.example.julestest.exception;

public class UnsupportedHashAlgorithmException extends RuntimeException {

    public UnsupportedHashAlgorithmException(String message) {
        super(message);
    }

    public UnsupportedHashAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}
