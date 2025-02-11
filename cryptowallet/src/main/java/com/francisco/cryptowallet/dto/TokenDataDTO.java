package com.francisco.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenDataDTO (
    String id,
    String rank,
    String symbol,
    String name,
    String supply,
    String maxSupply,
    String marketCapUsd,
    String volumeUsd24Hr,
    String priceUsd,
    String changePercent24Hr,
    String vwap24Hr
) {};
