package com.example.banking.service;
public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(String id) { super("Account already exists: " + id); }
}
