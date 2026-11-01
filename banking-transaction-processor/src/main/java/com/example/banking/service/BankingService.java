package com.example.banking.service;

import com.example.banking.api.dto.*;
import com.example.banking.domain.*;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionEntryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class BankingService {
    private final AccountRepository accounts;
    private final TransactionEntryRepository ledger;

    public BankingService(AccountRepository accounts, TransactionEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (request.openingBalance().scale() > 2) {
            throw new InvalidRequestException("openingBalance must have at most two decimal places");
        }
        if (accounts.existsById(request.id())) {
            throw new AccountAlreadyExistsException(request.id());
        }
        try {
            Account account = accounts.saveAndFlush(new Account(request.id(), request.openingBalance()));
            return new AccountResponse(account.getId(), account.getBalance());
        } catch (DataIntegrityViolationException ex) {
            // Handles the race where two requests create the same account concurrently.
            throw new AccountAlreadyExistsException(request.id());
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String id) {
        Account account = accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return new AccountResponse(account.getId(), account.getBalance());
    }

    @Transactional
    public AccountResponse deposit(String id, AmountRequest request) {
        validateMoney(request.amount());
        Account account = accounts.findByIdForUpdate(id).orElseThrow(() -> new AccountNotFoundException(id));
        account.credit(request.amount());
        accounts.save(account);
        record(account, TransactionType.DEPOSIT, request.amount(), UUID.randomUUID().toString(), request.description());
        return new AccountResponse(account.getId(), account.getBalance());
    }

    @Transactional
    public AccountResponse withdraw(String id, AmountRequest request) {
        validateMoney(request.amount());
        Account account = accounts.findByIdForUpdate(id).orElseThrow(() -> new AccountNotFoundException(id));
        account.debit(request.amount());
        accounts.save(account);
        record(account, TransactionType.WITHDRAWAL, request.amount(), UUID.randomUUID().toString(), request.description());
        return new AccountResponse(account.getId(), account.getBalance());
    }

    @Transactional
    public String transfer(TransferRequest request) {
        validateMoney(request.amount());
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new InvalidRequestException("Source and destination accounts must be different");
        }

        // Lock accounts in a stable order to reduce deadlocks for opposite-direction transfers.
        String firstId = request.fromAccountId().compareTo(request.toAccountId()) < 0
                ? request.fromAccountId() : request.toAccountId();
        String secondId = firstId.equals(request.fromAccountId())
                ? request.toAccountId() : request.fromAccountId();

        Account first = accounts.findByIdForUpdate(firstId).orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accounts.findByIdForUpdate(secondId).orElseThrow(() -> new AccountNotFoundException(secondId));
        Account source = first.getId().equals(request.fromAccountId()) ? first : second;
        Account destination = first.getId().equals(request.toAccountId()) ? first : second;

        source.debit(request.amount());
        destination.credit(request.amount());
        accounts.save(source);
        accounts.save(destination);

        String reference = UUID.randomUUID().toString();
        Instant now = Instant.now();
        ledger.save(new TransactionEntry(source.getId(), TransactionType.TRANSFER_DEBIT,
                request.amount(), source.getBalance(), reference, now, request.description()));
        ledger.save(new TransactionEntry(destination.getId(), TransactionType.TRANSFER_CREDIT,
                request.amount(), destination.getBalance(), reference, now, request.description()));
        return reference;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(String accountId, int page, int size) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("page must be >= 0 and size must be between 1 and 100");
        }
        return ledger.findByAccountIdOrderByOccurredAtDescIdDesc(
                        accountId, PageRequest.of(page, size, Sort.unsorted()))
                .map(e -> new TransactionResponse(e.getId(), e.getAccountId(), e.getType(), e.getAmount(),
                        e.getBalanceAfter(), e.getReferenceId(), e.getOccurredAt(), e.getDescription()));
    }

    private void record(Account account, TransactionType type, BigDecimal amount,
                        String reference, String description) {
        ledger.save(new TransactionEntry(account.getId(), type, amount, account.getBalance(),
                reference, Instant.now(), description));
    }

    private void validateMoney(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new InvalidRequestException("amount must be positive and have at most two decimal places");
        }
    }
}
