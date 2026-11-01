package com.example.banking.service;
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String id) { super("Account not found: " + id); }
}
