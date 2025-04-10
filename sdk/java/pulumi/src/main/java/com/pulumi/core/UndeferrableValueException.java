package com.pulumi.core;

public class UndeferrableValueException extends RuntimeException {
    public UndeferrableValueException(String message) {
        super(message);
    }
}
