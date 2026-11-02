package com.novabank.ops.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void 천단위_구분자와_공백을_걷어낸다() {
        assertEquals(new BigDecimal("1000000"), MoneyUtil.parse("1,000,000"));
        assertEquals(new BigDecimal("50000"), MoneyUtil.parse(" 50000 "));
        assertEquals(BigDecimal.ZERO, MoneyUtil.parse(""));
        assertEquals(BigDecimal.ZERO, MoneyUtil.parse(null));
    }

    @Test
    void 숫자가_아니면_예외() {
        assertThrows(IllegalArgumentException.class, () -> {
            MoneyUtil.parse("백만원");
        });
    }

    @Test
    void 원_단위_반올림() {
        assertEquals(new BigDecimal("101"), MoneyUtil.roundWon(new BigDecimal("100.5")));
        assertEquals(new BigDecimal("100"), MoneyUtil.roundWon(new BigDecimal("100.49")));
    }
}
