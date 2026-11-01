package com.example.banking;

import com.example.banking.api.dto.*;
import com.example.banking.domain.TransactionType;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionEntryRepository;
import com.example.banking.service.AccountNotFoundException;
import com.example.banking.service.BankingService;
import com.example.banking.domain.InsufficientFundsException;
import com.example.banking.service.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class BankingTransactionProcessorApplicationTests {
    @Autowired BankingService service;
    @Autowired AccountRepository accounts;
    @Autowired TransactionEntryRepository ledger;

    @BeforeEach
    void cleanDatabase() {
        ledger.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void createsAccountAndReturnsOpeningBalance() {
        AccountResponse response = service.createAccount(new CreateAccountRequest("A1", new BigDecimal("100.00")));
        assertThat(response.id()).isEqualTo("A1");
        assertThat(response.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void depositAndWithdrawalUpdateBalanceAndLedger() {
        service.createAccount(new CreateAccountRequest("A1", new BigDecimal("100.00")));
        service.deposit("A1", new AmountRequest(new BigDecimal("20.00"), "cash in"));
        service.withdraw("A1", new AmountRequest(new BigDecimal("30.00"), "cash out"));

        assertThat(service.getAccount("A1").balance()).isEqualByComparingTo("90.00");
        Page<TransactionResponse> history = service.history("A1", 0, 10);
        assertThat(history.getContent()).hasSize(2);
        assertThat(history.getContent()).extracting(TransactionResponse::type)
                .containsExactly(TransactionType.WITHDRAWAL, TransactionType.DEPOSIT);
    }

    @Test
    void transferUpdatesBothAccountsAndCreatesLinkedLedgerEntries() {
        service.createAccount(new CreateAccountRequest("A1", new BigDecimal("100.00")));
        service.createAccount(new CreateAccountRequest("A2", new BigDecimal("10.00")));

        String reference = service.transfer(new TransferRequest("A1", "A2", new BigDecimal("25.00"), "rent"));

        assertThat(service.getAccount("A1").balance()).isEqualByComparingTo("75.00");
        assertThat(service.getAccount("A2").balance()).isEqualByComparingTo("35.00");
        var sourceHistory = service.history("A1", 0, 10).getContent();
        var targetHistory = service.history("A2", 0, 10).getContent();
        assertThat(sourceHistory).singleElement().satisfies(e -> {
            assertThat(e.type()).isEqualTo(TransactionType.TRANSFER_DEBIT);
            assertThat(e.referenceId()).isEqualTo(reference);
        });
        assertThat(targetHistory).singleElement().satisfies(e -> {
            assertThat(e.type()).isEqualTo(TransactionType.TRANSFER_CREDIT);
            assertThat(e.referenceId()).isEqualTo(reference);
        });
    }

    @Test
    void rejectsOverdraftWithoutWritingLedgerEntry() {
        service.createAccount(new CreateAccountRequest("A1", new BigDecimal("10.00")));
        assertThatThrownBy(() -> service.withdraw("A1", new AmountRequest(new BigDecimal("11.00"), null)))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(service.getAccount("A1").balance()).isEqualByComparingTo("10.00");
        assertThat(service.history("A1", 0, 10).getContent()).isEmpty();
    }

    @Test
    void rejectsZeroAmount() {
        service.createAccount(new CreateAccountRequest("A1", new BigDecimal("10.00")));
        assertThatThrownBy(() -> service.deposit("A1", new AmountRequest(BigDecimal.ZERO, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsTransferToSameAccount() {
        service.createAccount(new CreateAccountRequest("A1", new BigDecimal("10.00")));
        assertThatThrownBy(() -> service.transfer(new TransferRequest("A1", "A1", new BigDecimal("1.00"), null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void missingAccountReturnsDomainError() {
        assertThatThrownBy(() -> service.getAccount("missing"))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
