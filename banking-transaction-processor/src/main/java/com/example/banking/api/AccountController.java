package com.example.banking.api;

import com.example.banking.api.dto.*;
import com.example.banking.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final BankingService service;

    public AccountController(BankingService service) { this.service = service; }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return service.createAccount(request);
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable String id) { return service.getAccount(id); }

    @PostMapping("/accounts/{id}/deposits")
    public AccountResponse deposit(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return service.deposit(id, request);
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public AccountResponse withdraw(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return service.withdraw(id, request);
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return new TransferResponse(service.transfer(request));
    }

    @GetMapping("/accounts/{id}/transactions")
    public Page<TransactionResponse> history(@PathVariable String id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.history(id, page, size);
    }

    public record TransferResponse(String referenceId) {}
}
