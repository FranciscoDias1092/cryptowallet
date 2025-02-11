package com.francisco.cryptowallet.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.francisco.cryptowallet.domain.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    
    Optional<Wallet> findByEmail(String email);

    Boolean existsByEmail(String email);
}
