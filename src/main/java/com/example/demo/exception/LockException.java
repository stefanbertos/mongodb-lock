package com.example.demo.exception;

public class LockException extends RuntimeException {
    public LockException(String message) {
        super(message);
    }
}