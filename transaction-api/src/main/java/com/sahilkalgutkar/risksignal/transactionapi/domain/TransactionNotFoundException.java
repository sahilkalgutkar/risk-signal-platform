package com.sahilkalgutkar.risksignal.transactionapi.domain;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String id) {
        super("No transaction found with id " + id);
    }
}
