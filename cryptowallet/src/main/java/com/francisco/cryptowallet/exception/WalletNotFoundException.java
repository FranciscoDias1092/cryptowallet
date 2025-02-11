package com.francisco.cryptowallet.exception;

public class WalletNotFoundException extends NotFoundException {

    public WalletNotFoundException() {
        super("Wallet not found!");
    }

    public WalletNotFoundException(String email) {
        super("Wallet not found for email: " + email + "!");
    }
}
