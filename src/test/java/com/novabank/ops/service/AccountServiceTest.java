package com.novabank.ops.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.AccountStatus;
import com.novabank.ops.domain.AccountType;
import com.novabank.ops.repository.InMemoryAccountRepository;

public class AccountServiceTest {

    private AccountService service;

    @Before
    public void setUp() {
        service = new AccountService();
        ReflectionTestUtils.setField(service, "accountRepository", new InMemoryAccountRepository());
    }

    @Test
    public void 표본_계좌_6개가_계좌번호순이다() {
        List<Account> accounts = service.findAll();
        assertEquals(6, accounts.size());
        assertEquals("110-0917-000001", accounts.get(0).getAccountNo());
        assertEquals("110-0917-000006", accounts.get(5).getAccountNo());
    }

    @Test
    public void 계좌_개설은_다음_일련번호를_받는다() {
        Account a = service.open("홍길동", "01", new BigDecimal("50000"));
        assertEquals("110-0917-000007", a.getAccountNo());
        assertEquals(AccountType.SAVINGS, a.getType());
        assertEquals(AccountStatus.ACTIVE, a.getStatus());
        assertEquals(7, service.findAll().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void 정기예금은_백만원_미만으로_개설_불가() {
        service.open("홍길동", "03", new BigDecimal("999999"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void 잔액이_있으면_해지_불가() {
        service.updateStatus("110-0917-000001", "CLOSED");
    }

    @Test
    public void 잔액_0원_계좌는_해지되고_되돌릴_수_없다() {
        Account a = service.updateStatus("110-0917-000005", "C");
        assertEquals(AccountStatus.CLOSED, a.getStatus());
        assertTrue(a.getClosedAt() != null);
        try {
            service.updateStatus("110-0917-000005", "ACTIVE");
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("해지 계좌가 되돌려졌다");
    }

    @Test(expected = IllegalArgumentException.class)
    public void 거래정지_계좌는_활성_계좌_조회에서_거부() {
        service.getActiveAccount("110-0917-000005");
    }

    @Test
    public void 총잔액은_해지_계좌를_뺀다() {
        service.updateStatus("110-0917-000005", "CLOSED");
        assertEquals(new BigDecimal("105480000"), service.totalBalance());
    }

    @Test
    public void 이름_마스킹() {
        assertEquals("김*수", service.getAccount("110-0917-000001").getMaskedName());
        assertEquals("(*****사", service.getAccount("110-0917-000006").getMaskedName());
    }
}
