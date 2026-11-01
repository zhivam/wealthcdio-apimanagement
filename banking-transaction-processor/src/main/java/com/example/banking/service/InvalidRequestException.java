package com.example.banking.service;
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) { super(message); }
}
