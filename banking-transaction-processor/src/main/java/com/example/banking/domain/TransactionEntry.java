package com.example.banking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_ledger", indexes = {
    @Index(name = "idx_ledger_account_timestamp", columnList = "account_id, occurred_at")
})
public class TransactionEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 36)
    private String referenceId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(length = 250)
    private String description;

    protected TransactionEntry() {}

    public TransactionEntry(String accountId, TransactionType type, BigDecimal amount,
                            BigDecimal balanceAfter, String referenceId, Instant occurredAt,
                            String description) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getReferenceId() { return referenceId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getDescription() { return description; }
}
