package com.example.banking.api.dto;

import java.math.BigDecimal;

public record AccountResponse(String id, BigDecimal balance) {}
