package com.francisco.cryptowallet.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.domain.Wallet;

@DataJpaTest
@ActiveProfiles("test")
public class WalletRepositoryTest {
    
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Token token;

    private Wallet wallet;

    private String email = "test@email.com";

    @BeforeEach
    void setUp() {
        token = Token.builder().id("bitcoin").symbol("BTC").price(1500.00).build();
        entityManager.persistAndFlush(token);

        wallet = Wallet.builder().email(email).build();
        entityManager.persistAndFlush(wallet);
    }

    @Test
    public void whenFindByEmail_thenReturnWalletWithEmptyAssetsList() {
        Optional<Wallet> walletOptional = walletRepository.findByEmail(email);

        assertTrue(walletOptional.isPresent());
        assertEquals(email, walletOptional.get().getEmail());
        assertEquals(null, walletOptional.get().getAssets());
    }

    @Test
    public void whenFindByEmail_thenReturnWalletWithAssets() {
        Asset asset = new Asset();
        asset.setWallet(wallet);
        asset.setQuantity(1.5);
        asset.setToken(token);

        wallet.setAssets(new ArrayList<>(List.of(asset)));

        entityManager.persistAndFlush(asset);

        Optional<Wallet> walletOptional = walletRepository.findByEmail(email);

        assertTrue(walletOptional.isPresent());
        assertEquals(email, walletOptional.get().getEmail());
        assertEquals(1, walletOptional.get().getAssets().size());
        assertEquals(1.5, walletOptional.get().getAssets().get(0).getQuantity());
    }

    @Test
    public void whenFindByEmail_thenReturnEmptyOptional() {
        Optional<Wallet> walletOptional = walletRepository.findByEmail("invalid@email.com");

        assertFalse(walletOptional.isPresent());
    }

    @Test
    public void whenExistsByEmail_thenReturnTrue() {
        Boolean walletExists = walletRepository.existsByEmail(email);

        assertTrue(walletExists);
    }

    @Test
    public void whenExistsByEmail_thenReturnFalse() {
        Boolean walletExists = walletRepository.existsByEmail("invalid@email.com");

        assertFalse(walletExists);
    }
}
