package com.gdb.Exceptions;

public class InsufficientBalanceException extends AccountException {
    public InsufficientBalanceException(String message) {

        super(message);
    }
}
