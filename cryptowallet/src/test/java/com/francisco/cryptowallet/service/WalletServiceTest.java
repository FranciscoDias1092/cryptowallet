package com.francisco.cryptowallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Wallet;
import com.francisco.cryptowallet.dto.WalletDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationResponseDTO;
import com.francisco.cryptowallet.exception.NotFoundException;
import com.francisco.cryptowallet.exception.WalletAlreadyExistsException;
import com.francisco.cryptowallet.exception.WalletNotFoundException;
import com.francisco.cryptowallet.mapper.WalletMapper;
import com.francisco.cryptowallet.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {
    
    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private Wallet wallet;
    private WalletDTO walletDto;
    private String email;

    @BeforeEach
    public void setUpService() {
        walletId = UUID.randomUUID();
        email = "test@email.com";
        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setEmail(email);
        wallet.setAssets(new ArrayList<>());

        walletDto = new WalletDTO(walletId, email, null, null);
    }

    @Test
    public void whenCreateWallet_thenWalletIsCreated() {
        when(walletRepository.existsByEmail(email)).thenReturn(false);
        when(walletMapper.walletToWalletDTO(any(Wallet.class))).thenReturn(walletDto);

        WalletDTO createdWallet = walletService.createWallet(email);

        assertNotNull(createdWallet);
        assertEquals(email, createdWallet.email());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    public void whenCreateWalletWithExistingEmail_thenThrowWalletAlreadyExistsException() {
        when(walletRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(WalletAlreadyExistsException.class, () -> walletService.createWallet(email));
        
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    public void whenGetWalletById_thenReturnWallet() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletMapper.walletToWalletDTO(wallet)).thenReturn(walletDto);

        WalletDTO retrievedWallet = walletService.getWallet(walletId);

        assertNotNull(retrievedWallet);
        assertEquals(walletId, retrievedWallet.id());
    }

    @Test
    public void whenGetWalletByIdNotFound_thenThrowWalletNotFoundException() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWallet(walletId));
    }

    @Test
    public void whenGetWalletByEmail_thenReturnWallet() {
        when(walletRepository.findByEmail(email)).thenReturn(Optional.of(wallet));
        when(walletMapper.walletToWalletDTO(wallet)).thenReturn(walletDto);

        WalletDTO retrievedWallet = walletService.getWallet(email);

        assertNotNull(retrievedWallet);
        assertEquals(email, retrievedWallet.email());
    }

    @Test
    public void whenGetWalletByEmailNotFound_thenThrowWalletNotFoundException() {
        when(walletRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWallet(email));
    }

    @Test
    public void whenEvaluateWallet_thenReturnEvaluationResponse() {
        LocalDate date = LocalDate.now();
        Asset asset = new Asset();
        asset.setWallet(wallet);

        wallet.getAssets().add(asset);

        WalletEvaluationResponseDTO responseDTO = new WalletEvaluationResponseDTO(5000.00, email, null, email, null);
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(tokenService.fetchHistoricalPrices(anyList(), eq(date))).thenReturn(Optional.of(responseDTO));

        WalletEvaluationResponseDTO result = walletService.evaluateWallet(walletId, date);

        assertNotNull(result);
        assertEquals(5000.00, result.total());
    }

    @Test
    public void whenEvaluateWalletNotFound_thenThrowWalletNotFoundException() {
        LocalDate date = LocalDate.now();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.evaluateWallet(walletId, date));
    }

    @Test
    public void whenEvaluateEmptyWallet_thenThrowNotFoundException() {
        LocalDate date = LocalDate.now();
        wallet.setAssets(List.of());
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThrows(NotFoundException.class, () -> walletService.evaluateWallet(walletId, date));
    }

    @Test
    public void whenEvaluateWalletNoResults_thenThrowNotFoundException() {
        LocalDate date = LocalDate.now();
        Asset asset = new Asset();
        wallet.setAssets(List.of(asset));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(tokenService.fetchHistoricalPrices(anyList(), eq(date))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> walletService.evaluateWallet(walletId, date));
    }
}
