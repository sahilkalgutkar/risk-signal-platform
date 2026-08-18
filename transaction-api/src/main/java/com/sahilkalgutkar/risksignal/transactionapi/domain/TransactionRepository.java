package com.sahilkalgutkar.risksignal.transactionapi.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
