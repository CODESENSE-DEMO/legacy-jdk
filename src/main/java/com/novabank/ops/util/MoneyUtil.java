package com.novabank.ops.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.lang3.StringUtils;

/**
 * 금액 유틸. 원 단위 정수 금액만 다룬다.
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** "1,000,000" · " 50000 " 같은 입력을 금액으로. 비어 있으면 0 */
    public static BigDecimal parse(String text) {
        if (StringUtils.isBlank(text)) {
            return BigDecimal.ZERO;
        }
        String digits = text.replace(",", "").trim();
        try {
            return new BigDecimal(digits).setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("금액 형식이 올바르지 않습니다: " + text, e);
        }
    }

    public static BigDecimal won(long value) {
        return new BigDecimal(value);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    /** 원 단위 반올림 */
    public static BigDecimal roundWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
