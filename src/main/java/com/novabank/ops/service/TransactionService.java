package com.novabank.ops.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.Transaction;
import com.novabank.ops.domain.TxType;
import com.novabank.ops.repository.TransactionRepository;
import com.novabank.ops.util.MoneyUtil;
import com.novabank.ops.util.TxIdGenerator;

/**
 * 입출금·거래 내역 서비스. 잔액 갱신은 메모리 저장소 기준이라 서비스 메서드를 동기화한다.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final Comparator<Transaction> BY_CREATED_DESC = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction a, Transaction b) {
            int byTime = b.getCreatedAt().compareTo(a.getCreatedAt());
            // 같은 밀리초에 기록된 거래는 일련번호로 순서를 가른다
            return byTime != 0 ? byTime : Long.valueOf(b.getSeq()).compareTo(Long.valueOf(a.getSeq()));
        }
    };

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TxIdGenerator txIdGenerator;

    @Value("${withdraw.limit.per.tx:10000000}")
    private long withdrawLimitPerTx = 10000000L;

    public synchronized Transaction deposit(String accountNo, BigDecimal amount, String memo) {
        requirePositive(amount);
        Account account = accountService.getActiveAccount(accountNo);
        BigDecimal after = account.getBalance().add(amount);
        accountService.applyBalance(account, after);
        return record(account, TxType.DEPOSIT, amount, after, null, memo);
    }

    public synchronized Transaction withdraw(String accountNo, BigDecimal amount, String memo) {
        requirePositive(amount);
        if (amount.compareTo(MoneyUtil.won(withdrawLimitPerTx)) > 0) {
            throw new IllegalArgumentException("1회 출금 한도를 초과했습니다. 한도 " + withdrawLimitPerTx + "원");
        }
        Account account = accountService.getActiveAccount(accountNo);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다. 잔액 " + account.getBalance() + "원, 요청 " + amount + "원");
        }
        BigDecimal after = account.getBalance().subtract(amount);
        accountService.applyBalance(account, after);
        return record(account, TxType.WITHDRAW, amount, after, null, memo);
    }

    /** 이체 서비스가 쓰는 원장 기록 (잔액은 호출자가 이미 반영) */
    Transaction record(Account account, TxType type, BigDecimal amount, BigDecimal balanceAfter,
                       String counterparty, String memo) {
        Transaction tx = new Transaction();
        tx.setTxId(newTxId());
        tx.setAccountNo(account.getAccountNo());
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setCounterparty(counterparty);
        tx.setMemo(memo == null ? "" : memo.trim());
        tx.setCreatedAt(new Date());
        transactionRepository.save(tx);
        log.info("거래 기록 {}", tx);
        return tx;
    }

    public List<Transaction> history(String accountNo) {
        List<Transaction> list = transactionRepository.findByAccountNo(accountNo);
        Collections.sort(list, BY_CREATED_DESC);
        return list;
    }

    public List<Transaction> findAll() {
        List<Transaction> list = transactionRepository.findAll();
        Collections.sort(list, BY_CREATED_DESC);
        return list;
    }

    private String newTxId() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String id = txIdGenerator.generate();
            if (!transactionRepository.existsTxId(id)) {
                return id;
            }
        }
        throw new IllegalStateException("거래번호를 생성하지 못했습니다(중복 10회).");
    }

    static void requirePositive(BigDecimal amount) {
        if (!MoneyUtil.isPositive(amount)) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        if (amount.scale() > 0 && amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("금액은 원 단위 정수여야 합니다: " + amount);
        }
    }
}
