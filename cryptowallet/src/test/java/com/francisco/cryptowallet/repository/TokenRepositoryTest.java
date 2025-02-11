package com.francisco.cryptowallet.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.francisco.cryptowallet.domain.Token;

@DataJpaTest
@ActiveProfiles("test")
public class TokenRepositoryTest {
    
    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Token token;

    @BeforeEach
    void setUp() {
        token = Token.builder().id("bitcoin").symbol("BTC").price(1500.00).build();
        entityManager.persistAndFlush(token);
    }

    @Test
    public void whenExistsByEmail_thenReturnTrue() {
        Optional<Token> tokenOptional = tokenRepository.findBySymbol("BTC");

        assertTrue(tokenOptional.isPresent());
        assertEquals("bitcoin", tokenOptional.get().getId());
        assertEquals("BTC", tokenOptional.get().getSymbol());
        assertEquals(1500.00, tokenOptional.get().getPrice());
    }

    @Test
    public void whenExistsByEmail_thenReturnFalse() {
        Optional<Token> tokenOptional = tokenRepository.findBySymbol("ETH");

        assertFalse(tokenOptional.isPresent());
    }
}
