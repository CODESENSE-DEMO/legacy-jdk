package com.novabank.ops.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.novabank.ops.domain.Transaction;
import com.novabank.ops.domain.TxType;
import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.repository.InMemoryTransactionRepository;
import com.novabank.ops.util.TxIdGenerator;

public class TransactionServiceTest {

    static final String ACC = "110-0917-000001";   // 김민수 3,250,000

    private TransactionService service;
    private AccountService accountService;

    @Before
    public void setUp() {
        accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());
        service = new TransactionService();
        ReflectionTestUtils.setField(service, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "txIdGenerator", new TxIdGenerator());
    }

    @Test
    public void 입금하면_잔액이_늘고_내역이_남는다() {
        Transaction tx = service.deposit(ACC, new BigDecimal("150000"), "급여");
        assertEquals(TxType.DEPOSIT, tx.getType());
        assertEquals(new BigDecimal("3400000"), tx.getBalanceAfter());
        assertEquals(new BigDecimal("3400000"), accountService.getAccount(ACC).getBalance());
        assertEquals(1, service.history(ACC).size());
        assertEquals(10, tx.getTxId().length());
    }

    @Test
    public void 출금하면_잔액이_준다() {
        Transaction tx = service.withdraw(ACC, new BigDecimal("250000"), null);
        assertEquals(new BigDecimal("3000000"), tx.getBalanceAfter());
        assertEquals("", tx.getMemo());
    }

    @Test(expected = IllegalArgumentException.class)
    public void 잔액보다_많이_출금하면_예외() {
        service.withdraw(ACC, new BigDecimal("3250001"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 출금_한도_초과는_예외() {
        service.withdraw("110-0917-000006", new BigDecimal("10000001"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 거래정지_계좌는_입금_불가() {
        service.deposit("110-0917-000005", new BigDecimal("1000"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 금액_0은_예외() {
        service.deposit(ACC, BigDecimal.ZERO, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 원_단위가_아니면_예외() {
        service.deposit(ACC, new BigDecimal("100.5"), null);
    }

    @Test
    public void 내역은_최신순이다() {
        service.deposit(ACC, new BigDecimal("1000"), "1");
        service.deposit(ACC, new BigDecimal("2000"), "2");
        service.deposit(ACC, new BigDecimal("3000"), "3");
        assertEquals("3", service.history(ACC).get(0).getMemo());
    }
}
