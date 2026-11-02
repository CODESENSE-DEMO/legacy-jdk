package com.novabank.ops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.novabank.ops.domain.LoanInstallment;
import com.novabank.ops.domain.LoanSchedule;

public class LoanCalculatorServiceTest {

    private final LoanCalculatorService service = new LoanCalculatorService();

    @Test
    public void 원리금균등_천만원_연12_12개월() {
        LoanSchedule s = service.schedule(new BigDecimal("10000000"), new BigDecimal("12"), 12, "EQUAL_PAYMENT");
        assertEquals(12, s.getInstallments().size());
        // 월 1% × 12회 → 매월 888,488원
        assertEquals(new BigDecimal("888488"), s.getInstallments().get(0).getPayment());
        assertEquals(new BigDecimal("100000"), s.getInstallments().get(0).getInterest());
        assertEquals(BigDecimal.ZERO, s.getInstallments().get(11).getBalance());
        assertTrue(s.getTotalInterest().compareTo(new BigDecimal("661000")) > 0
                && s.getTotalInterest().compareTo(new BigDecimal("663000")) < 0);
        assertEquals(s.getPrincipal().add(s.getTotalInterest()), s.getTotalPayment());
    }

    @Test
    public void 원금균등은_원금이_같고_이자가_줄어든다() {
        LoanSchedule s = service.schedule(new BigDecimal("12000000"), new BigDecimal("6"), 12, "EQUAL_PRINCIPAL");
        LoanInstallment first = s.getInstallments().get(0);
        LoanInstallment last = s.getInstallments().get(11);
        assertEquals(new BigDecimal("1000000"), first.getPrincipal());
        assertEquals(new BigDecimal("60000"), first.getInterest());
        assertEquals(new BigDecimal("5000"), last.getInterest());
        assertEquals(BigDecimal.ZERO, last.getBalance());
        assertEquals(new BigDecimal("390000"), s.getTotalInterest());
    }

    @Test
    public void 원금이_나누어_떨어지지_않아도_마지막_회차가_맞춘다() {
        LoanSchedule s = service.schedule(new BigDecimal("1000000"), new BigDecimal("5"), 7, "EQUAL_PRINCIPAL");
        BigDecimal sum = BigDecimal.ZERO;
        for (LoanInstallment i : s.getInstallments()) {
            sum = sum.add(i.getPrincipal());
        }
        assertEquals(new BigDecimal("1000000"), sum);
        assertEquals(BigDecimal.ZERO, s.getInstallments().get(6).getBalance());
    }

    @Test
    public void 방식_미지정은_원리금균등() {
        assertEquals("EQUAL_PAYMENT", service.schedule(new BigDecimal("1000000"), new BigDecimal("3"), 6, null).getMethod());
    }

    @Test
    void 이율_범위_밖은_예외() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.schedule(new BigDecimal("1000000"), new BigDecimal("25"), 12, null);
        });
    }

    @Test
    void 개월_수_상한_초과는_예외() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.schedule(new BigDecimal("1000000"), new BigDecimal("3"), 481, null);
        });
    }
}
