package com.novabank.ops.repository;

import java.util.List;

import com.novabank.ops.domain.Account;

/**
 * 계좌 저장소.
 */
public interface AccountRepository {

    List<Account> findAll();

    Account findByAccountNo(String accountNo);

    void save(Account account);

    /** 다음 계좌 일련번호(지점 내 6자리) */
    int nextSequence();
}
