package com.francisco.cryptowallet.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.francisco.cryptowallet.domain.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByWalletId(UUID walletId);
}
