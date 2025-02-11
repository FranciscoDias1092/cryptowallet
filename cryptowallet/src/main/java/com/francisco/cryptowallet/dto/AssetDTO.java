package com.francisco.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetDTO (String symbol, double quantity, double price, double value) {};
