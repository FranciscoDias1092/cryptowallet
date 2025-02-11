package com.francisco.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetHistoryDataDTO (String priceUsd, Long time) {};
