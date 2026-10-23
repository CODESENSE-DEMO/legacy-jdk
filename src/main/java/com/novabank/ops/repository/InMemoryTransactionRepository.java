package com.novabank.ops.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.novabank.ops.domain.Transaction;

/**
 * 메모리 거래 내역 저장소.
 */
@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> store = new ConcurrentHashMap<String, Transaction>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<Transaction>(store.values());
    }

    @Override
    public List<Transaction> findByAccountNo(String accountNo) {
        List<Transaction> result = new ArrayList<Transaction>();
        for (Transaction tx : store.values()) {
            if (tx.getAccountNo().equals(accountNo)) {
                result.add(tx);
            }
        }
        return result;
    }

    @Override
    public void save(Transaction tx) {
        if (tx.getSeq() == 0) {
            tx.setSeq(sequence.incrementAndGet());
        }
        store.put(tx.getTxId(), tx);
    }

    @Override
    public boolean existsTxId(String txId) {
        return txId != null && store.containsKey(txId);
    }
}
