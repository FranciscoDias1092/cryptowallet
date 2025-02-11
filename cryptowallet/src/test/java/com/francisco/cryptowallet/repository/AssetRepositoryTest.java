package com.francisco.cryptowallet.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

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
public class AssetRepositoryTest {
    
    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void whenFindByWalletId_thenReturnAssets() {
        Wallet wallet = Wallet.builder().email("test@email.com").build();
        entityManager.persistAndFlush(wallet);

        Token token = Token.builder().id("bitcoin").symbol("BTC").price(1500.00).build();
        entityManager.persistAndFlush(token);

        Asset asset = new Asset();
        asset.setWallet(wallet);
        asset.setQuantity(1.5);
        asset.setToken(token);

        entityManager.persist(asset);
        entityManager.flush();

        List<Asset> assets = assetRepository.findByWalletId(wallet.getId());

        assertFalse(assets.isEmpty());
        assertEquals(1, assets.size());
        assertEquals(asset.getWallet().getId(), assets.get(0).getWallet().getId());
        assertEquals(1.5 * 1500.00, assets.get(0).getValue());
    }
}
