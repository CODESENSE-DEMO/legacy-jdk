package com.novabank.ops.repository;

import java.util.List;

import com.novabank.ops.domain.Transaction;

/**
 * 거래 내역 저장소.
 */
public interface TransactionRepository {

    List<Transaction> findAll();

    List<Transaction> findByAccountNo(String accountNo);

    void save(Transaction tx);

    boolean existsTxId(String txId);
}
