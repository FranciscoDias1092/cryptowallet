package com.francisco.cryptowallet.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WalletDTO (UUID id, String email, Double total, List<AssetDTO> assets) {};
