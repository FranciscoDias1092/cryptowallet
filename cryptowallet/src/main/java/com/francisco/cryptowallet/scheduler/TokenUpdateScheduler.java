package com.francisco.cryptowallet.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.francisco.cryptowallet.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A scheduled task to fetch the last Token prices and update the saved Tokens.
 * 
 * It triggers {@link TokenService#updateAllTokenPrices()} at a set interval.
 * 
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenUpdateScheduler {
    
    private final TokenService tokenService;

    /**
     * Token prices are fetched and updated at a fixed interval.
     * 
     * This interval is configured in {@code token.price.update.interval}.
     * 
     */
    @Scheduled(fixedRateString = "${token.price.update.interval}")
    public void fetchTokenPrices() {
        log.info("Triggering scheduled token price update.");
        tokenService.updateAllTokenPrices();
    }
}
