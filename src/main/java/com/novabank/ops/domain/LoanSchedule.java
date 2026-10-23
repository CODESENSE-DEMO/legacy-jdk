package com.novabank.ops.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 대출 상환 스케줄(원리금균등 / 원금균등).
 */
public class LoanSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EQUAL_PAYMENT = "EQUAL_PAYMENT";
    public static final String EQUAL_PRINCIPAL = "EQUAL_PRINCIPAL";

    private String method;
    private BigDecimal principal;
    private BigDecimal annualRatePercent;
    private int months;
    private BigDecimal totalInterest = BigDecimal.ZERO;
    private BigDecimal totalPayment = BigDecimal.ZERO;
    private List<LoanInstallment> installments = new ArrayList<LoanInstallment>();

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethodLabel() {
        return EQUAL_PRINCIPAL.equals(method) ? "원금균등" : "원리금균등";
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getAnnualRatePercent() {
        return annualRatePercent;
    }

    public void setAnnualRatePercent(BigDecimal annualRatePercent) {
        this.annualRatePercent = annualRatePercent;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment) {
        this.totalPayment = totalPayment;
    }

    public List<LoanInstallment> getInstallments() {
        return installments;
    }

    public void setInstallments(List<LoanInstallment> installments) {
        this.installments = installments;
    }
}
