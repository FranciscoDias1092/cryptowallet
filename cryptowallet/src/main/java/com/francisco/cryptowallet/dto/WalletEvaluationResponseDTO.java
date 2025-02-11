package com.francisco.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WalletEvaluationResponseDTO (
    Double total, 
    String best_asset, 
    Double best_performance, 
    String worst_asset,
    Double worst_performance
) {};