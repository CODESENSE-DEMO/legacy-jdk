package com.novabank.ops.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.novabank.ops.domain.LoanInstallment;
import com.novabank.ops.domain.LoanSchedule;
import com.novabank.ops.util.MoneyUtil;

/**
 * 대출 상환 스케줄 계산기(원리금균등·원금균등). 창구 상담용이라 원장과 무관하다.
 */
@Service
public class LoanCalculatorService {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    @Value("${loan.rate.min.percent:1.0}")
    private double rateMinPercent = 1.0;

    @Value("${loan.rate.max.percent:20.0}")
    private double rateMaxPercent = 20.0;

    @Value("${loan.months.max:480}")
    private int monthsMax = 480;

    public LoanSchedule schedule(BigDecimal principal, BigDecimal annualRatePercent, int months, String method) {
        if (!MoneyUtil.isPositive(principal)) {
            throw new IllegalArgumentException("대출 원금은 0보다 커야 합니다.");
        }
        if (annualRatePercent == null
                || annualRatePercent.compareTo(new BigDecimal(rateMinPercent)) < 0
                || annualRatePercent.compareTo(new BigDecimal(rateMaxPercent)) > 0) {
            throw new IllegalArgumentException("연이율은 " + rateMinPercent + "% ~ " + rateMaxPercent + "% 사이여야 합니다.");
        }
        if (months < 1 || months > monthsMax) {
            throw new IllegalArgumentException("상환 개월 수는 1 ~ " + monthsMax + " 사이여야 합니다.");
        }
        String m = method == null ? LoanSchedule.EQUAL_PAYMENT : method.trim().toUpperCase();
        if (!LoanSchedule.EQUAL_PAYMENT.equals(m) && !LoanSchedule.EQUAL_PRINCIPAL.equals(m)) {
            throw new IllegalArgumentException("알 수 없는 상환 방식: " + method);
        }

        LoanSchedule schedule = new LoanSchedule();
        schedule.setMethod(m);
        schedule.setPrincipal(principal);
        schedule.setAnnualRatePercent(annualRatePercent);
        schedule.setMonths(months);

        // 월 이율 = 연이율 / 12 / 100
        BigDecimal monthlyRate = annualRatePercent.divide(new BigDecimal(1200), MC);
        if (LoanSchedule.EQUAL_PAYMENT.equals(m)) {
            buildEqualPayment(schedule, monthlyRate);
        } else {
            buildEqualPrincipal(schedule, monthlyRate);
        }
        return schedule;
    }

    /** 원리금균등: 매월 납입액 = P × r × (1+r)^n / ((1+r)^n − 1) */
    private void buildEqualPayment(LoanSchedule s, BigDecimal r) {
        BigDecimal principal = s.getPrincipal();
        int n = s.getMonths();
        BigDecimal payment;
        if (r.signum() == 0) {
            payment = principal.divide(new BigDecimal(n), 0, RoundingMode.HALF_UP);
        } else {
            BigDecimal q = BigDecimal.ONE.add(r).pow(n, MC);
            payment = MoneyUtil.roundWon(principal.multiply(r).multiply(q).divide(q.subtract(BigDecimal.ONE), MC));
        }
        BigDecimal balance = principal;
        for (int i = 1; i <= n; i++) {
            BigDecimal interest = MoneyUtil.roundWon(balance.multiply(r));
            BigDecimal principalPart = payment.subtract(interest);
            BigDecimal thisPayment = payment;
            if (i == n || principalPart.compareTo(balance) > 0) {
                // 마지막 회차는 잔액을 정확히 0 으로
                principalPart = balance;
                thisPayment = principalPart.add(interest);
            }
            balance = balance.subtract(principalPart);
            s.getInstallments().add(new LoanInstallment(i, thisPayment, principalPart, interest, balance));
            s.setTotalInterest(s.getTotalInterest().add(interest));
            s.setTotalPayment(s.getTotalPayment().add(thisPayment));
        }
    }

    /** 원금균등: 매월 원금 = P / n, 이자는 잔액 기준 */
    private void buildEqualPrincipal(LoanSchedule s, BigDecimal r) {
        BigDecimal principal = s.getPrincipal();
        int n = s.getMonths();
        BigDecimal principalPart = principal.divide(new BigDecimal(n), 0, RoundingMode.DOWN);
        BigDecimal balance = principal;
        for (int i = 1; i <= n; i++) {
            BigDecimal interest = MoneyUtil.roundWon(balance.multiply(r));
            BigDecimal thisPrincipal = (i == n) ? balance : principalPart;
            BigDecimal payment = thisPrincipal.add(interest);
            balance = balance.subtract(thisPrincipal);
            s.getInstallments().add(new LoanInstallment(i, payment, thisPrincipal, interest, balance));
            s.setTotalInterest(s.getTotalInterest().add(interest));
            s.setTotalPayment(s.getTotalPayment().add(payment));
        }
    }
}
