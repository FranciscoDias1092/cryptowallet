package com.francisco.cryptowallet.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.francisco.cryptowallet.dto.AssetDTO;
import com.francisco.cryptowallet.exception.GlobalExceptionHandler;
import com.francisco.cryptowallet.service.AssetService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {
    
    private final AssetService assetService;

    /**
     * Add an asset to an existing wallet (wallet identified by its email).
     * 
     * @param email if given an invalid email, a {@link HandlerMethodValidationException} is 
     * thrown that is handled in {@link GlobalExceptionHandler} (bad request returned)
     * @param assetDto
     * @return the new asset's info
     */
    @PostMapping("/email/{email}")
    public AssetDTO addAsset(@PathVariable @Email String email, @RequestBody @Valid AssetDTO assetDto) {
        return assetService.addAsset(email, assetDto);
    }

    /**
     * Add an asset to an existing wallet (wallet identified by its UUID).
     * 
     * @param id
     * @param assetDto
     * @return the new asset's info
     */
    @PostMapping("/id/{id}")
    public AssetDTO addAsset(@PathVariable UUID id, @RequestBody @Valid AssetDTO assetDto) {
        return assetService.addAsset(id, assetDto);
    }
}
