package com.francisco.cryptowallet.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.domain.Wallet;
import com.francisco.cryptowallet.dto.AssetDTO;
import com.francisco.cryptowallet.dto.WalletDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationRequestDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationResponseDTO;
import com.francisco.cryptowallet.exception.GlobalExceptionHandler;
import com.francisco.cryptowallet.exception.NotFoundException;
import com.francisco.cryptowallet.exception.WalletAlreadyExistsException;
import com.francisco.cryptowallet.exception.WalletNotFoundException;
import com.francisco.cryptowallet.mapper.WalletMapper;
import com.francisco.cryptowallet.repository.TokenRepository;
import com.francisco.cryptowallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    
    private final WalletRepository walletRepository;

    private final TokenRepository tokenRepository;

    private final TokenService tokenService;
    
    private final WalletMapper walletMapper;

    /**
     * Create a new wallet with the given email.
     * 
     * If a wallet with given email already exists, throw {@link WalletAlreadyExistsException} that is
     * handled in {@link GlobalExceptionHandler} (BAD_REQUEST returned) as there can only 
     * be one wallet per email address.
     * 
     * @param email
     * @return the new wallet's info
     */
    public WalletDTO createWallet(String email) {
        if (walletRepository.existsByEmail(email)) {
            throw new WalletAlreadyExistsException(email);
        }

        Wallet wallet = new Wallet();
        wallet.setEmail(email);
        walletRepository.save(wallet);

        return walletMapper.walletToWalletDTO(wallet);
    }

    /**
     * Retrieve a wallet by its UUID.
     * 
     * If a wallet with given UUID doesn't exist, throw {@link WalletNotFoundException} that is
     * handled in {@link GlobalExceptionHandler} (BAD_REQUEST returned). 
     * 
     * @param id
     * @return the wallet's info with its assets
     */
    public WalletDTO getWallet(UUID id) {
        Wallet wallet = walletRepository.findById(id).orElseThrow(() -> new WalletNotFoundException());
        return walletMapper.walletToWalletDTO(wallet);
    }

    /**
     * Retrieve a wallet by its email.
     * 
     * If a wallet with given email doesn't exist, throw {@link WalletNotFoundException} that is
     * handled in {@link GlobalExceptionHandler} (BAD_REQUEST returned).
     * 
     * @param email
     * @return the wallet's info with its assets
     */
    public WalletDTO getWallet(String email) {
        Wallet wallet = walletRepository.findByEmail(email).orElseThrow(() -> new WalletNotFoundException(email));
        return walletMapper.walletToWalletDTO(wallet);
    }

    /**
     * Return the wallet's performance results containing total wallet value (at date), 
     * and best and worst performing assets (and their respective performance as positive
     * or negative percentage).
     * 
     * If a wallet with given UUID doesn't exist, throw {@link WalletNotFoundException} that is
     * handled in {@link GlobalExceptionHandler} (BAD_REQUEST returned).
     * 
     * If wallet is empty (no assets) it is not evaluate and a {@link NotFoundException} is thrown
     * and handled in {@link GlobalExceptionHandler} (NOT_FOUND returned).
     * 
     * If no historical prices exist, a NotFoundException is also thrown.
     * 
     * @param id
     * @param date
     * @return the evaluation results containing total value (at date), best and worst performers (and respective performance)
     */
    public WalletEvaluationResponseDTO evaluateWallet(UUID id, LocalDate date) {
        Wallet wallet = walletRepository.findById(id)
                            .orElseThrow(() -> new WalletNotFoundException());
        
        List<Asset> assets = wallet.getAssets();
        
        if (assets.isEmpty()) {
            throw new NotFoundException("Wallet is empty!");
        }

        Optional<WalletEvaluationResponseDTO> responseOptional = tokenService.fetchHistoricalPrices(assets, date);

        if (!responseOptional.isPresent()) {
            throw new NotFoundException("No results to show!");
        }

        return responseOptional.get();
    }

    /**
     * The Map tokenSymbolIdMap is needed because the given assets only have the Token's symbol.
     * 
     * However, to consume the historic pricing service provided by the external
     * API (see {@link TokenService#fetchHistoricalPrices(List, LocalDate, Map)})
     * the token's id is needed, not its symbol. So, all tokens are fetched from the DB,
     * and tokenSymbolIdMap is created to map each tokens' symbol to its correspoding 
     * id. 
     * Note: Both the token's id and symbol are unique.
     * 
     * @param request
     * @param date
     * @return the wallet's performance results
     */
    public WalletEvaluationResponseDTO evaluateWallet(WalletEvaluationRequestDTO request, LocalDate date) {
        List<AssetDTO> assets = request.assets();

        if (assets.isEmpty()) {
            throw new NotFoundException("Wallet is empty!");
        }

        List<String> symbols = assets.stream().map(AssetDTO::symbol).collect(Collectors.toList());

        Map<String, String> tokenSymbolIdMap = tokenRepository.findAllBySymbolIn(symbols)
                .stream().collect(Collectors.toMap(Token::getSymbol, Token::getId));

        Set<String> storedTokenSymbols = tokenSymbolIdMap.keySet();
        List<String> missingSymbols = symbols.stream().filter(symbol -> !storedTokenSymbols.contains(symbol)).toList();

        // If some tokens don't exist in DB, fetch their respective details and save all missing tokens in DB.
        if (!missingSymbols.isEmpty()) {
            List<Token> newlyFetchedTokens = new ArrayList<>();

            for (String symbol : missingSymbols) {
                tokenService.fetchTokenDetails(symbol).ifPresent(token -> {
                    newlyFetchedTokens.add(token);
                    tokenSymbolIdMap.put(token.getSymbol(), token.getId());
                });
            }

            if(!newlyFetchedTokens.isEmpty()) {
                tokenRepository.saveAll(newlyFetchedTokens);
            }
        }

        Optional<WalletEvaluationResponseDTO> responseOptional = tokenService.fetchHistoricalPrices(assets, date, tokenSymbolIdMap);

        if (!responseOptional.isPresent()) {
            throw new NotFoundException("No results to show!");
        }

        return responseOptional.get();
    }
}
