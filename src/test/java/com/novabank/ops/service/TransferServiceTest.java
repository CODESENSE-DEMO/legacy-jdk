package com.novabank.ops.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.novabank.ops.domain.TransferResult;
import com.novabank.ops.domain.TxType;
import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.repository.InMemoryTransactionRepository;
import com.novabank.ops.util.TxIdGenerator;

public class TransferServiceTest {

    static final String FROM = "110-0917-000006";  // (주)한빛상사 58,900,000
    static final String TO = "110-0917-000003";    // 박지훈 850,000

    private TransferService service;
    private AccountService accountService;
    private TransactionService transactionService;

    @Before
    public void setUp() {
        accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());
        transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(transactionService, "accountService", accountService);
        ReflectionTestUtils.setField(transactionService, "txIdGenerator", new TxIdGenerator());
        service = new TransferService();
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "transactionService", transactionService);
    }

    private static Date weekdayAt(int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, Calendar.NOVEMBER, 13, hour, 0, 0);   // 수요일
        return cal.getTime();
    }

    @Test
    public void 당행_이체는_수수료_없이_양쪽_잔액이_움직인다() {
        TransferResult r = service.transfer(FROM, TO, "NOVA", new BigDecimal("1000000"), "거래대금");
        assertEquals(BigDecimal.ZERO, r.getFee());
        assertEquals(new BigDecimal("57900000"), accountService.getAccount(FROM).getBalance());
        assertEquals(new BigDecimal("1850000"), accountService.getAccount(TO).getBalance());
        assertEquals(TxType.TRANSFER_IN, transactionService.history(TO).get(0).getType());
        assertEquals(1, transactionService.history(FROM).size());   // 수수료 거래 없음
    }

    @Test
    public void 타행_이체_수수료는_금액_구간별이고_영업시간_외_가산() {
        assertEquals(new BigDecimal("500"), service.calculateFee("KDB", new BigDecimal("99999"), weekdayAt(10)));
        assertEquals(new BigDecimal("800"), service.calculateFee("KDB", new BigDecimal("100000"), weekdayAt(10)));
        assertEquals(new BigDecimal("1500"), service.calculateFee("KDB", new BigDecimal("1000000"), weekdayAt(10)));
        assertEquals(new BigDecimal("1800"), service.calculateFee("KDB", new BigDecimal("1000000"), weekdayAt(20)));
        assertEquals(BigDecimal.ZERO, service.calculateFee("", new BigDecimal("1000000"), weekdayAt(20)));
    }

    @Test
    public void 타행_이체는_수수료_거래가_따로_남는다() {
        TransferResult r = service.transfer(FROM, "123-456-789012", "IBK", new BigDecimal("500000"), null);
        assertEquals(r.getAmount().add(r.getFee()), r.getTotalDebit());
        assertEquals(2, transactionService.history(FROM).size());
        assertEquals(TxType.FEE, transactionService.history(FROM).get(0).getType());
        assertEquals(new BigDecimal("58900000").subtract(r.getTotalDebit()), accountService.getAccount(FROM).getBalance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void 일_한도_초과는_예외() {
        service.transfer(FROM, TO, "NOVA", new BigDecimal("50000000"), null);
        service.transfer(TO, FROM, "NOVA", new BigDecimal("50000000"), null);   // 되돌려서 잔액 확보
        service.transfer(FROM, TO, "NOVA", new BigDecimal("50000000"), null);
        service.transfer(TO, FROM, "NOVA", new BigDecimal("50000000"), null);
        service.transfer(FROM, TO, "NOVA", new BigDecimal("1"), null);   // 누계 100,000,001
    }

    @Test(expected = IllegalArgumentException.class)
    public void 회당_한도_초과는_예외() {
        service.transfer(FROM, TO, "NOVA", new BigDecimal("50000001"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 같은_계좌로는_이체_불가() {
        service.transfer(FROM, FROM, "NOVA", new BigDecimal("1000"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 거래정지_계좌로는_입금_불가() {
        service.transfer(FROM, "110-0917-000005", "NOVA", new BigDecimal("1000"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 수수료_포함_잔액_부족은_예외() {
        service.transfer(TO, "123-456-789012", "IBK", new BigDecimal("850000"), null);   // 잔액 850,000 + 수수료
    }
}
