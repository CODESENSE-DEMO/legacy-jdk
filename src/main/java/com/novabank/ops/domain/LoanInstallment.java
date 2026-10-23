package com.novabank.ops.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 대출 상환 스케줄 한 회차.
 */
public class LoanInstallment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int month;
    private BigDecimal payment;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal balance;

    public LoanInstallment() {
    }

    public LoanInstallment(int month, BigDecimal payment, BigDecimal principal, BigDecimal interest, BigDecimal balance) {
        this.month = month;
        this.payment = payment;
        this.principal = principal;
        this.interest = interest;
        this.balance = balance;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public void setPayment(BigDecimal payment) {
        this.payment = payment;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    public void setInterest(BigDecimal interest) {
        this.interest = interest;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
