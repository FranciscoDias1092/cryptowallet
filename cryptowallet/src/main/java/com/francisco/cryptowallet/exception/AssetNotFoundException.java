package com.francisco.cryptowallet.exception;

public class AssetNotFoundException extends NotFoundException {
    
    public AssetNotFoundException() {
        super("Asset not found!");
    }
}
