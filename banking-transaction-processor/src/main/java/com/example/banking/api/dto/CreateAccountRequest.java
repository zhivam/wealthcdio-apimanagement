package com.example.banking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String id,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) @Digits(integer = 17, fraction = 2)
        BigDecimal openingBalance) {}
