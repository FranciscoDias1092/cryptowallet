package com.francisco.cryptowallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequestDTO (@NotBlank @Email String email) {};
