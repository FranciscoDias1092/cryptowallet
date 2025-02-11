package com.francisco.cryptowallet.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.domain.Wallet;
import com.francisco.cryptowallet.dto.AssetDTO;
import com.francisco.cryptowallet.exception.TokenPriceException;
import com.francisco.cryptowallet.exception.WalletNotFoundException;
import com.francisco.cryptowallet.mapper.AssetMapper;
import com.francisco.cryptowallet.repository.AssetRepository;
import com.francisco.cryptowallet.repository.TokenRepository;
import com.francisco.cryptowallet.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
public class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    AssetService assetService;

    private Wallet wallet;
    private AssetDTO assetDto;
    private Token token;
    private Asset asset;

    @BeforeEach
    public void setUpService() {
        wallet = Wallet.builder().email("test@email.com").build();
        assetDto = AssetDTO.builder().symbol("BTC").quantity(1.7).price(1700.00).build();
        token = Token.builder().id("bitcoin").symbol("BTC").price(1700.00).build();
        asset = new Asset();
        asset.setWallet(wallet);
        asset.setToken(token);
        asset.setQuantity(1.7);
    }

    @Test
    public void whenAddAssetWalletNotFound_thenThrowException() {
        when(walletRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());
    
        assertThrows(WalletNotFoundException.class, () -> assetService.addAsset("test@email.com", assetDto));
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    public void whenTokenFetchFails_thenThrowException() {
        when(walletRepository.findByEmail("test@email.com")).thenReturn(Optional.of(wallet));
        when(tokenRepository.findBySymbol("BTC")).thenReturn(Optional.empty());
        when(tokenService.fetchTokenDetails("BTC")).thenReturn(Optional.empty());
    
        assertThrows(TokenPriceException.class, () -> assetService.addAsset("test@email.com", assetDto));
        verify(assetRepository, never()).save(any(Asset.class));
    }
}
