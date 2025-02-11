package com.francisco.cryptowallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.francisco.cryptowallet.domain.Token;

public interface TokenRepository extends JpaRepository<Token, String>{
    
    Optional<Token> findBySymbol(String symbol);

    List<Token> findAllBySymbolIn(List<String> symbol);
}
