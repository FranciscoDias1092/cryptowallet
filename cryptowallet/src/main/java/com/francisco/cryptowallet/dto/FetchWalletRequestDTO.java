package com.francisco.cryptowallet.dto;

import org.hibernate.validator.constraints.UUID;

import jakarta.validation.constraints.NotBlank;

public record FetchWalletRequestDTO (@NotBlank @UUID String id) {};
