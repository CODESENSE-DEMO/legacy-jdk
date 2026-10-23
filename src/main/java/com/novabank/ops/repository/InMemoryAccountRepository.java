package com.novabank.ops.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Repository;

import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.AccountStatus;
import com.novabank.ops.domain.AccountType;

/**
 * 메모리 계좌 저장소. 기동 시 표본 계좌 6개를 심는다.
 * (계정계 DB 연동 전 단계의 참조 구현 — 재기동하면 초기화된다)
 */
@Repository
public class InMemoryAccountRepository implements AccountRepository {

    public static final String BRANCH_CODE = "0917";

    private final Map<String, Account> store = new ConcurrentHashMap<String, Account>();
    private final AtomicInteger sequence = new AtomicInteger(0);

    public InMemoryAccountRepository() {
        seed();
    }

    private void seed() {
        add("김민수", AccountType.SAVINGS, "3250000", 2011, 3, 14, AccountStatus.ACTIVE);
        add("이서연", AccountType.CHECKING, "12480000", 2012, 7, 2, AccountStatus.ACTIVE);
        add("박지훈", AccountType.SAVINGS, "850000", 2012, 11, 20, AccountStatus.ACTIVE);
        add("최유진", AccountType.FIXED, "30000000", 2013, 1, 8, AccountStatus.ACTIVE);
        add("정우성", AccountType.SAVINGS, "0", 2010, 5, 30, AccountStatus.FROZEN);
        add("(주)한빛상사", AccountType.CHECKING, "58900000", 2009, 9, 1, AccountStatus.ACTIVE);
    }

    private void add(String name, AccountType type, String balance, int y, int m, int d, AccountStatus status) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(y, m - 1, d, 9, 30, 0);
        String no = formatAccountNo(sequence.incrementAndGet());
        Account account = new Account(no, name, type, new BigDecimal(balance), cal.getTime());
        account.setStatus(status);
        store.put(no, account);
    }

    /** 110-0917-000001 형식 */
    public static String formatAccountNo(int seq) {
        return "110-" + BRANCH_CODE + "-" + String.format("%06d", seq);
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<Account>(store.values());
    }

    @Override
    public Account findByAccountNo(String accountNo) {
        if (accountNo == null) {
            return null;
        }
        return store.get(accountNo.trim());
    }

    @Override
    public void save(Account account) {
        store.put(account.getAccountNo(), account);
    }

    @Override
    public int nextSequence() {
        return sequence.incrementAndGet();
    }

    public Date now() {
        return new Date();
    }
}
