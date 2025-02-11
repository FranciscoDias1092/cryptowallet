package com.francisco.cryptowallet.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.domain.Wallet;
import com.francisco.cryptowallet.dto.AssetDTO;
import com.francisco.cryptowallet.exception.GlobalExceptionHandler;
import com.francisco.cryptowallet.exception.TokenPriceException;
import com.francisco.cryptowallet.exception.WalletNotFoundException;
import com.francisco.cryptowallet.mapper.AssetMapper;
import com.francisco.cryptowallet.repository.AssetRepository;
import com.francisco.cryptowallet.repository.TokenRepository;
import com.francisco.cryptowallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {
    
    private final AssetRepository assetRepository;

    private final TokenRepository tokenRepository;

    private final WalletRepository walletRepository;

    private final TokenService tokenService;
    
    private final AssetMapper assetMapper;

    /**
     * Wallet will be retrieved by the provided email.
     * 
     * 
     * If wallet is not found, throws {@link WalletNotFoundException} to be 
     * handled in {@link GlobalExceptionHandler} (NOT_FOUND returned).
     * 
     * Asset processing is delegated to {@link #processAsset(Optional, AssetDTO)}.
     * 
     * @param email
     * @param assetDto
     * @return
     */
    public AssetDTO addAsset(String email, AssetDTO assetDto) {
        Wallet wallet = walletRepository.findByEmail(email).orElseThrow(WalletNotFoundException::new);
        return processAsset(wallet, assetDto);
    }

    /**
     * Wallet will be retrieved by the provided UUID.
     * 
     * 
     * If wallet is not found, throws {@link WalletNotFoundException} to be 
     * handled in {@link GlobalExceptionHandler} (NOT_FOUND returned).
     * 
     * Asset processing is delegated to {@link #processAsset(Optional, AssetDTO)}.
     * 
     * @param id
     * @param assetDto
     * @return
     */
    public AssetDTO addAsset(UUID id, AssetDTO assetDto) {
        Wallet wallet = walletRepository.findById(id).orElseThrow(WalletNotFoundException::new);
        return processAsset(wallet, assetDto);
    }

    /**
     * Retrieve the token.
     * 
     * Create and save a Asset object.
     * 
     * @param walletOptional
     * @param assetDto
     * @return up-to-date asset's info
     */
    private AssetDTO processAsset(Wallet wallet, AssetDTO assetDto) {
        Token token = getToken(assetDto.symbol());

        Asset asset = Asset.builder()
                        .token(token)
                        .wallet(wallet)
                        .quantity(assetDto.quantity())
                        .build();

        assetRepository.save(asset);

        return assetMapper.assetToAssetDTO(asset);
    };

    /**
     * If the token already exists in the DB, will call external API using 
     * the token's id (/assets/{{id}}), the prices is updated in the DB.
     * 
     * If the token doens't exist in the DB, the external API will be called
     * (/assets?search=symbol&limit=1) to search the token's details (including its price) using the symbol
     * specified in the request and search=symbol limit=1 to get only the exact correspondence,
     * and the token is saved in the DB using the retrieved detailts (most importantly, its id). 
     * 
     * @param symbol (for example, "BTC" or "ETH")
     * @return token corresponding to the symbol (for example, "BTC", "ETH") which is unique.
     */
    public Token getToken(String symbol) {
        return tokenRepository.findBySymbol(symbol)
                .map(this::updatePrice)
                .orElseGet(() -> fetchAndSave(symbol));
    }

    /**
     * Call external API using the token's id (/assets/{{id}}), 
     * if the up-to-date price is retrieved, the token's price 
     * is updated in the DB.
     * 
     * @param token
     * @return updated Token.
     */
    private Token updatePrice(Token token) {
        Optional<Double> priceOptional = tokenService.fetchTokenPrice(token.getId());

        priceOptional.ifPresent(price -> {
            token.setPrice(price);
            tokenRepository.save(token);
        });

        return token;
    }

    /**
     * Call external API using the token's symbol (/assets/{{id}}), 
     * if the up-to-date price is retrieved, the token with up-to-date
     * price is saved and retrieved.
     * 
     * If nothing is fetched, a {@link TokenPriceException} is thrown and handled in
     * {@link GlobalExceptionHandler} (BAD_REQUEST returned).
     * 
     * @param symbol (for example, "BTC" or "ETH")
     * @return newly created Token with up-to-date price.
     */
    private Token fetchAndSave(String symbol) {
        Token token = tokenService.fetchTokenDetails(symbol)
                        .orElseThrow(() -> new TokenPriceException("Could not fetch token details for " + symbol + "!"));

        tokenRepository.save(token);
        
        return token;
    }
}
