package com.example.banking.api.dto;

import com.example.banking.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long id, String accountId, TransactionType type,
                                  BigDecimal amount, BigDecimal balanceAfter,
                                  String referenceId, Instant occurredAt, String description) {}
