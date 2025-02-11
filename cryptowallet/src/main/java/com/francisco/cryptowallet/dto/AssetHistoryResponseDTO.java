package com.francisco.cryptowallet.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetHistoryResponseDTO (List<AssetHistoryDataDTO> data) {};
