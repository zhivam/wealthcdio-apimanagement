package com.example.banking.repository;

import com.example.banking.domain.TransactionEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {
    Page<TransactionEntry> findByAccountIdOrderByOccurredAtDescIdDesc(String accountId, Pageable pageable);
}
