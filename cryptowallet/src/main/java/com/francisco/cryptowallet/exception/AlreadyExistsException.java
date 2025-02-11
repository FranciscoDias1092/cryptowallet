package com.francisco.cryptowallet.exception;

public class AlreadyExistsException extends RuntimeException {
    
    public AlreadyExistsException(String message) {
        super(message);
    }
}
