package com.francisco.cryptowallet.exception;

public class WalletAlreadyExistsException extends AlreadyExistsException {
    
    public WalletAlreadyExistsException(String email) {
        super("A wallet already exists for the email: " + email + "!");
    }
}
