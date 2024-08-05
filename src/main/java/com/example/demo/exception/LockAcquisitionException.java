package com.example.demo.exception;

public class LockAcquisitionException extends LockException {
    public LockAcquisitionException(String message) {
        super(message);
    }
}