package com.gdb.Exceptions;

public class InvalidAmountException extends AccountException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
