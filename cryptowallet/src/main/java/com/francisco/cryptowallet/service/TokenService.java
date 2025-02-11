package com.francisco.cryptowallet.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.dto.AssetDTO;
import com.francisco.cryptowallet.dto.AssetHistoryResponseDTO;
import com.francisco.cryptowallet.dto.TokenDataDTO;
import com.francisco.cryptowallet.dto.TokenListResponseDTO;
import com.francisco.cryptowallet.dto.TokenResponseDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationResponseDTO;
import com.francisco.cryptowallet.exception.GlobalExceptionHandler;
import com.francisco.cryptowallet.exception.TokenPriceException;
import com.francisco.cryptowallet.repository.TokenRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage Tokens and their prices.
 * 
 * Token prices are fetched from an external API, and the tokens are updated in the DB.
 * 
 * Historial prices are also fetched from the external API to provide historical
 * price and performance analysis for wallet assets.
 * 
 * Parallel processing is used for efficiency.
 * 
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    
    private final TokenRepository tokenRepository;

    private final RestTemplate restTemplate;

    @Value("${token.price.thread.max}")
    private int maxConcurrentThreads;

    @Value("${token.price.retry.max}")
    private int maxRetries;

    @Value("${token.price.retry.delay}")
    private long retrayDelay;

    @Value("${token.api.url.assets}")
    private String apiAssetsUrl;
    
    private ExecutorService executorService;

    private ExecutorService historyExecutorService;

    String assetsHistoryUrl;
    
    /**
     * Thread pools are initialized and the asset history URL is constructed.
     * 
     * The number of threads in the pool is set in {@code token.price.thread.max}
     * 
     */
    @PostConstruct
    private void init() {
        executorService = Executors.newFixedThreadPool(maxConcurrentThreads);
        historyExecutorService = Executors.newFixedThreadPool(10);
        assetsHistoryUrl = apiAssetsUrl + "/{id}/history";
    }

    /**
     * Shut down thread pools when the service is destroyed.
     */
    @PreDestroy
    private void shutdown() {
        executorService.shutdown();
        historyExecutorService.shutdown();
    }

    /**
     * Update the price of all tokens stored in the database asynchronously.
     * 
     * If there are no saved Tokens, the task ends (nothing to update).
     * 
     */
    public void updateAllTokenPrices() {
        log.info("Starting token price update...");

        List<Token> tokens = tokenRepository.findAll();

        if (tokens.isEmpty()) {
            log.warn("No tokens found. Skipping update.");
            return;
        }

        // Asynchronously update prices for each token.
        List<CompletableFuture<Void>> futures = tokens.stream()
                    .map(token -> CompletableFuture.runAsync(() -> fetchAndUpdateTokenPrice(token), executorService))
                    .collect(Collectors.toList());

        // Wait for all futures to complete.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Completed scheduled token price update!");
    }

    /**
     * Fetch and update a token's price, handling failures with logging.
     * 
     * @param token to update
     */
    protected void fetchAndUpdateTokenPrice(Token token) {
        try {
            Double price = fetchTokenPriceWithRetry(token);
            token.setPrice(price);
            tokenRepository.save(token);

            log.info("Updated {}'s price to {}", token.getSymbol(), token.getPrice());
        } catch (TokenPriceException e) {
            log.warn("Failed to update price for {} after all retries.", token.getSymbol());
        }
    }

    /**
     * Attempts to fetch the token's price, with retry logic.
     * 
     * The maximum number of retries is set in {@Code token.price.retry.max} and the
     * delay (ms) is set in {@Code token.price.retry.delay}.
     * 
     * @param token
     * @return the fetched up-to-date price
     * @throws {@link TokenPriceException} if all retry attempts fail
     */
    protected Double fetchTokenPriceWithRetry(Token token) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Optional<Double> priceOptional = fetchTokenPrice(token.getId());

                if (priceOptional.isPresent()) {
                    return priceOptional.get();
                } else {
                    log.warn("Attempt {}/{}: Price not found for {}. Retrying...",
                        attempt, maxRetries, token.getSymbol());
                }
            } catch (RestClientException e) {
                log.warn("Attempt {}/{} failed for {}: {}",
                    attempt, maxRetries, token.getSymbol(), e.getMessage());
            }

            if (attempt < maxRetries) {
                sleep(retrayDelay * attempt);
            }
        }

        throw new TokenPriceException("Failed to fetch price for " + token.getSymbol() + ".");
    }

    /**
     * Fetch the latest price of a token from the external API.
     * 
     * If a {@link HttpClientErrorException} is caught, the error is logged and
     * a {@link TokenPriceException} is thrown and handled in {@link GlobalExceptionHandler}.
     * 
     * If a {@link Exception} is caught, the error is logged and
     * a {@link TokenPriceException} is also thrown and handled in {@link GlobalExceptionHandler}.
     * 
     * @param id
     * @return an optional containing the price
     */
    public Optional<Double> fetchTokenPrice(String id) {
        String url = apiAssetsUrl + "/" + id.toLowerCase();
        
        try {
            TokenResponseDTO response = restTemplate.getForObject(url, TokenResponseDTO.class);
            
            return Optional.ofNullable(response)
                           .map(TokenResponseDTO::data)
                           .flatMap(this::parsePrice);
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching price for {}: {}", id, e.getMessage());
            throw new TokenPriceException("Failed to fetch token price for " + id + "!");
        } catch (RestClientException e) {
            log.error("Client error fetching price for {}: {}", id, e.getMessage());
            throw new TokenPriceException("Failed to fetch token price for " + id + "!");
        } catch (Exception e) {
            log.error("Unexpected error fetching token price for {}: {}", id, e.getMessage());
            throw new TokenPriceException("An unexpected error occurred while fetching " + id + "'s price!");
        } 
    }

    /**
     * Pause execution.
     * 
     * @param milliseconds to sleep
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread {} while sleeping. While proceed with retry.", Thread.currentThread().getName());
        }
    }

    /**
     * Fetch token details from the external API by the provided symbol.
     * 
     * The token doens't exist in the DB, and the external API is called
     * (/assets?search=symbol&limit=1) to search the token's details (including its price) using the symbol
     * specified in the request and search=symbol limit=1 to get only the exact correspondence 
     * because all tokens whose symbol contain the symbol parameters are returned.
     * 
     * In case of an error, the error is logged, and an empty Optional is returned.
     * 
     * @param symbol (for example, "BTC", "ETH")
     * @return an Optional containing the token details or an empty Optional
     */
    public Optional<Token> fetchTokenDetails(String symbol) {
        String url = UriComponentsBuilder.fromUriString(apiAssetsUrl)
                        .queryParam("search", symbol.toUpperCase())
                        .queryParam("limit", 1)
                        .build()
                        .toString();

        try {
            TokenListResponseDTO response = restTemplate.getForObject(url, TokenListResponseDTO.class);

            if (response != null && response.data() != null) {
                TokenDataDTO data = response.data().get(0);

                Token token = new Token();
                token.setId(data.id());
                token.setSymbol(data.symbol());
                token.setPrice(Double.parseDouble(data.priceUsd()));

                return Optional.of(token);
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching details for {}: {}", symbol, e.getMessage());
        } catch (RestClientException e) {
            log.error("Client error fetching details for {}: {}", symbol, e.getMessage());
        } catch (NumberFormatException e) {
            log.error("Invalid price format for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching token details for {}: {}", symbol, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Fetch historical token prices for a list of assets.
     * 
     * Retrieves historical price data for each token in the list,
     * calculates the tolta wallet value at the specified date, and 
     * identifies the best and worst performing tokens while also registering
     * their respective performances (change percentage).
     * 
     * Uses parallel execution with {@link CompletableFuture} to speed the process.
     * 
     * Returns an empty Optional is no data is found.
     * 
     * @param assets a List
     * @param date the starting date
     * @return an Optional containing the total value stored in the wallet at the specfied
     * date, and the best and worst performing tokens (with their respective performances).
     */
    public Optional<WalletEvaluationResponseDTO> fetchHistoricalPrices(List<Asset> assets, LocalDate date) {
        // Uses Atomic references to store values safely accross multiple threads.
        AtomicReference<Double> totalValue = new AtomicReference<>(0.0);
        AtomicReference<String> bestPerformer = new AtomicReference<>(null);
        AtomicReference<String> worstPerformer = new AtomicReference<>(null);
        AtomicReference<Double> bestPerformance = new AtomicReference<>(Double.NEGATIVE_INFINITY);
        AtomicReference<Double> worstPerformance = new AtomicReference<>(Double.POSITIVE_INFINITY);   

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime startDateTime = date.atTime(currentTime.getHour(), currentTime.getMinute(), 0);

        // To fetch the historic pricing, the start date is set to the specified date (yyyy-MM-dd)
        // with the hours and minutes (hh::mm::00) being taken from currentTime.
        long startLong = startDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        
        // End date will is set to 1 minute after the start date. Only the price at start date is
        // relevant for calculating the performance of the wallet, and the 1 minute interval allows 
        // for the price point to be obtained.
        long endLong = startDateTime.plusMinutes(1).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        String start = String.valueOf(startLong);
        String end = String.valueOf(endLong);

        // Lock object for thread safety during updates.
        Object lock = new Object();

        List<CompletableFuture<Void>> futures = assets.stream()
                .map(asset -> CompletableFuture.runAsync(() -> {
                    String tokenId = asset.getToken().getId();
                    // Construct the url.
                    String url = UriComponentsBuilder.fromUriString(assetsHistoryUrl.replace("{id}", tokenId))
                                .queryParam("interval", "m1")
                                .queryParam("start", start)
                                .queryParam("end", end)
                                .build()
                                .toString();

                    try  {
                        AssetHistoryResponseDTO response = 
                            restTemplate.getForObject(url, AssetHistoryResponseDTO.class);
        
                        if (response != null && response.data() != null && !response.data().isEmpty()) {
                            double pastPrice = parsePrice(response.data().get(0).priceUsd());

                            synchronized(lock) {
                                double assetPrice = asset.getToken().getPrice();
                                // Add the result of the asset's quantity * the past price (at specified date) 
                                // from the list to the total wallet value.
                                totalValue.updateAndGet(value -> value + asset.getQuantity() * pastPrice);
                            
                                if (pastPrice > 0) {
                                    // Calculate the percentage change the asset's performance between past date
                                    // and current date. Result is positive if the price increased,
                                    // negative if price decreased in that period.
                                    double performance = (assetPrice - pastPrice) / pastPrice * 100.0;
            
                                    // If true, update the best performing asset and the best performance.
                                    if (performance > bestPerformance.get()) {
                                        bestPerformance.set(performance);
                                        bestPerformer.set(asset.getToken().getSymbol());
                                    } 
                                    
                                    // If true, update the worst performing asset and the worst performance.
                                    if (performance < worstPerformance.get()) {
                                        worstPerformance.set(performance);
                                        worstPerformer.set(asset.getToken().getSymbol());
                                    }
                                }
                            }
                        }
                    } catch (HttpClientErrorException e) {
                        log.error("HTTP error fetching historical price for {}: {}", asset.getToken().getSymbol(), e.getMessage());
                    } catch (RestClientException e) {
                        log.error("Client error fetching historical price for {}: {}", asset.getToken().getSymbol(), e.getMessage());
                    } catch (Exception e) {
                        log.error("Unexpected error fetching historical price for {}: {}", asset.getToken().getSymbol(), e.getMessage());
                    } 
                }, historyExecutorService))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Unexpected error occured while waiting for historical price tasks to complete: {}", e.getMessage());
        }

        return bestPerformance.get() != Double.NEGATIVE_INFINITY ? 
            Optional.of(WalletEvaluationResponseDTO.builder()
                    .total(totalValue.get())
                    .best_asset(bestPerformer.get())
                    .best_performance(Math.round(bestPerformance.get() * 100.0) / 100.0)
                    .worst_asset(worstPerformer.get())
                    .worst_performance(Math.round(worstPerformance.get() * 100.0) / 100.0)
                    .build()) : 
            Optional.empty();
    }

    /**
     * Fetch historical token prices for a list of assets.
     * 
     * Retrieves historical price data for each token in the list,
     * calculates the tolta wallet value at the specified date, and 
     * identifies the best and worst performing tokens while also registering
     * their respective performances (change percentage).
     * 
     * Uses parallel execution with {@link CompletableFuture} to speed the process.
     * 
     * Returns an empty Optional is no data is found.
     * 
     * @param assets a List
     * @param date the starting date
     * @return an Optional containing the total value stored in the wallet at the specfied
     * date, and the best and worst performing tokens (with their respective performances).
     */
    public Optional<WalletEvaluationResponseDTO> fetchHistoricalPrices(List<AssetDTO> assets, LocalDate date, Map<String, String> tokenSymbolIdMap) {
        // Uses Atomic references to store values safely accross multiple threads.
        AtomicReference<Double> totalValue = new AtomicReference<>(0.0);
        AtomicReference<String> bestPerformer = new AtomicReference<>(null);
        AtomicReference<String> worstPerformer = new AtomicReference<>(null);
        AtomicReference<Double> bestPerformance = new AtomicReference<>(Double.NEGATIVE_INFINITY);
        AtomicReference<Double> worstPerformance = new AtomicReference<>(Double.POSITIVE_INFINITY);   

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime startDateTime = date.atTime(currentTime.getHour(), currentTime.getMinute(), 0);

        // To fetch the historic pricing, the start date is set to the specified date (yyyy-MM-dd)
        // with the hours and minutes (hh::mm::00) being taken from currentTime.
        long startLong = startDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        
        // End date will is set to 1 minute after the start date. Only the price at start date is
        // relevant for calculating the performance of the wallet, and the 1 minute interval allows 
        // for the price point to be obtained.
        long endLong = startDateTime.plusMinutes(1).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        String start = String.valueOf(startLong);
        String end = String.valueOf(endLong);

        // Lock object for thread safety during updates.
        Object lock = new Object();

        List<CompletableFuture<Void>> futures = assets.stream()
                .map(asset -> CompletableFuture.runAsync(() -> {
                    String tokenId = tokenSymbolIdMap.get(asset.symbol());
                    // Construct the url.
                    // The interval is set to m1 as only the first historic record is necessary and m1 is
                    // the minimum interval permit for the external API.
                    String url = UriComponentsBuilder.fromUriString(assetsHistoryUrl.replace("{id}", tokenId))
                                .queryParam("interval", "m1")
                                .queryParam("start", start)
                                .queryParam("end", end)
                                .build()
                                .toString();

                    try  {
                        AssetHistoryResponseDTO response = 
                            restTemplate.getForObject(url, AssetHistoryResponseDTO.class);
        
                        if (response != null && response.data() != null && !response.data().isEmpty()) {
                            double pastPrice = parsePrice(response.data().get(0).priceUsd());

                            synchronized(lock) {
                                double assetPrice = asset.value()/asset.quantity();
                                // Add the result of the asset's quantity * the past price (at specified date) 
                                // from the list to the total wallet value.
                                totalValue.updateAndGet(value -> value + asset.quantity() * pastPrice);
                            
                                if (pastPrice > 0) {
                                    // Calculate the percentage change the asset's performance between past date
                                    // and current date. Result is positive if the price increased,
                                    // negative if price decreased in that period.
                                    double performance = (assetPrice - pastPrice) / pastPrice * 100.0;
            
                                    // If true, update the best performing asset and the best performance.
                                    if (performance > bestPerformance.get()) {
                                        bestPerformance.set(performance);
                                        bestPerformer.set(asset.symbol());
                                    } 
                                    
                                    // If true, update the worst performing asset and the worst performance.
                                    if (performance < worstPerformance.get()) {
                                        worstPerformance.set(performance);
                                        worstPerformer.set(asset.symbol());
                                    }
                                }
                            }
                        }
                    } catch (HttpClientErrorException e) {
                        log.error("HTTP error fetching historical price for {}: {}", asset.symbol(), e.getMessage());
                    } catch (RestClientException e) {
                        log.error("Client error fetching historical price for {}: {}", asset.symbol(), e.getMessage());
                    } catch (Exception e) {
                        log.error("Unexpected error fetching historical price for {}: {}", asset.symbol(), e.getMessage());
                    } 
                }, historyExecutorService))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Unexpected error occured while waiting for historical price tasks to complete: {}", e.getMessage());
        }

        return bestPerformance.get() != Double.NEGATIVE_INFINITY ? 
            Optional.of(WalletEvaluationResponseDTO.builder()
                    .total(totalValue.get())
                    .best_asset(bestPerformer.get())
                    .best_performance(Math.round(bestPerformance.get() * 100.0) / 100.0)
                    .worst_asset(worstPerformer.get())
                    .worst_performance(Math.round(worstPerformance.get() * 100.0) / 100.0)
                    .build()) : 
            Optional.empty();
    }

    private Optional<Double> parsePrice(TokenDataDTO tokenData) {
        try {
            return Optional.ofNullable(tokenData.priceUsd())
                        .map(Double::parseDouble);
        } catch (NumberFormatException e) {
            log.error("Failed to parse price for {}: {}", tokenData.symbol(), e.getMessage());
            return Optional.empty();
        }
    }

    private double parsePrice(String price) {
        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}