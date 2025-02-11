package com.francisco.cryptowallet.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.francisco.cryptowallet.dto.CreateWalletRequestDTO;
import com.francisco.cryptowallet.dto.FetchWalletRequestDTO;
import com.francisco.cryptowallet.dto.WalletDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationRequestDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationResponseDTO;
import com.francisco.cryptowallet.exception.GlobalExceptionHandler;
import com.francisco.cryptowallet.service.WalletService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {
    
    private final WalletService walletService;

    /**
     * Create a new empty wallet with the given email.
     * 
     * @param email if given an invalid email, a {@link HandlerMethodValidationException} is 
     * thrown that is handled in {@link GlobalExceptionHandler} (bad request returned)
     * @return an empty wallet
     */
    @PostMapping("/create/{email}")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDTO createWallet(@PathVariable @Email String email) {
        return walletService.createWallet(email);
    }

    /**
     * Create a new empty wallet with the given email.
     * 
     * @param email if given an invalid email, a {@link MethodArgumentNotValidException} is 
     * thrown that is handled in {@link GlobalExceptionHandler} (bad request returned)
     * @return an empty wallet
     */
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDTO createWallet(@RequestBody @Valid CreateWalletRequestDTO walletDTO) {
        return walletService.createWallet(walletDTO.email());
    }
    
    /**
     * Retrieve a wallet by it's id.
     * 
     * @param id
     * @return the wallet and its assets
     */
    @GetMapping("/id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public WalletDTO getWallet(@PathVariable UUID id) {
        return walletService.getWallet(id);
    }

    /**
     * Retrieve a wallet by the user's email. 
     * 
     * @param email
     * @return the wallet and its assets
     */
    @GetMapping("/email/{email}")
    @ResponseStatus(HttpStatus.OK)
    public WalletDTO getWallet(@PathVariable String email) {
        return walletService.getWallet(email);
    }

    /**
     * Retrieve a wallet by the its UUID. 
     * 
     * @param email
     * @return the wallet and its assets
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public WalletDTO getWallet(@RequestBody @Valid FetchWalletRequestDTO request) {
        return walletService.getWallet(UUID.fromString(request.id()));
    }

    /**
     * Evaluate a wallet's assets. 
     * 
     * Show the best and worst performing assets and the total
     * value in the wallet for the date parameter.
     * 
     * @param id
     * @param date has format yyyy-MM-dd
     * @return the evaluation results
     */
    @GetMapping("/evaluate/{id}")
    @ResponseStatus(HttpStatus.OK)
    public WalletEvaluationResponseDTO evaluateWallet(@PathVariable UUID id, @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return walletService.evaluateWallet(id, date);
    }

    /**
     * Evaluate assets performance. 
     * 
     * Show the best and worst performing assets and the total
     * value in the wallet for the date parameter.
     * 
     * @param date
     * @param assetDTOs
     * @return
     */
    @GetMapping("/evaluate")
    @ResponseStatus(HttpStatus.OK)
    public WalletEvaluationResponseDTO evaluateWallet(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date, @RequestBody WalletEvaluationRequestDTO assetDTOs) {
        return walletService.evaluateWallet(assetDTOs, date);
    }
}
