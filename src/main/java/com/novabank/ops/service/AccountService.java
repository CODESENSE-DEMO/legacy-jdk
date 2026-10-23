package com.novabank.ops.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.AccountStatus;
import com.novabank.ops.domain.AccountType;
import com.novabank.ops.repository.AccountRepository;
import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.util.MoneyUtil;

/**
 * 계좌 관리 서비스.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    /** 계좌번호 오름차순 */
    private static final Comparator<Account> BY_ACCOUNT_NO = new Comparator<Account>() {
        @Override
        public int compare(Account a, Account b) {
            return a.getAccountNo().compareTo(b.getAccountNo());
        }
    };

    @Autowired
    private AccountRepository accountRepository;

    public List<Account> findAll() {
        List<Account> accounts = accountRepository.findAll();
        Collections.sort(accounts, BY_ACCOUNT_NO);
        return accounts;
    }

    public Account getAccount(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo);
        if (account == null) {
            throw new IllegalArgumentException("존재하지 않는 계좌입니다: " + accountNo);
        }
        return account;
    }

    /** 활성 계좌만 (이체 대상 선택 등) */
    public Account getActiveAccount(String accountNo) {
        Account account = getAccount(accountNo);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("거래할 수 없는 계좌 상태입니다(" + account.getStatus().getLabel() + "): " + accountNo);
        }
        return account;
    }

    public synchronized Account open(String customerName, String typeCode, BigDecimal initialDeposit) {
        Validate.isTrue(StringUtils.isNotBlank(customerName), "예금주 이름은 필수입니다.");
        if (initialDeposit == null || initialDeposit.signum() < 0) {
            throw new IllegalArgumentException("최초 입금액은 0 이상이어야 합니다.");
        }
        AccountType type = AccountType.fromCode(typeCode);
        if (type == AccountType.FIXED && initialDeposit.compareTo(MoneyUtil.won(1000000)) < 0) {
            throw new IllegalArgumentException("정기예금은 최초 입금액이 1,000,000원 이상이어야 합니다.");
        }
        String accountNo = InMemoryAccountRepository.formatAccountNo(accountRepository.nextSequence());
        Account account = new Account(accountNo, customerName.trim(), type, initialDeposit, new Date());
        accountRepository.save(account);
        log.info("계좌 개설 {}", account);
        return account;
    }

    public Account updateStatus(String accountNo, String statusCode) {
        Account account = getAccount(accountNo);
        AccountStatus status = AccountStatus.fromCode(statusCode);
        if (status == AccountStatus.CLOSED) {
            if (account.getBalance().signum() != 0) {
                throw new IllegalArgumentException("잔액이 남아 있는 계좌는 해지할 수 없습니다. 잔액 " + account.getBalance() + "원");
            }
            account.setClosedAt(new Date());
        }
        if (account.getStatus() == AccountStatus.CLOSED && status != AccountStatus.CLOSED) {
            throw new IllegalArgumentException("해지된 계좌는 상태를 되돌릴 수 없습니다: " + accountNo);
        }
        account.setStatus(status);
        accountRepository.save(account);
        log.info("계좌 상태 변경 {} -> {}", accountNo, status);
        return account;
    }

    /** 잔액 반영 (거래 서비스가 호출) */
    void applyBalance(Account account, BigDecimal newBalance) {
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    public BigDecimal totalBalance() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Account a : accountRepository.findAll()) {
            if (a.getStatus() != AccountStatus.CLOSED) {
                sum = sum.add(a.getBalance());
            }
        }
        return sum;
    }
}
