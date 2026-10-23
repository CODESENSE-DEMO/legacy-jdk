package com.novabank.ops.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 일마감 리포트. 본점 집계 시스템으로 XML 로 전송되며 화면에서는 JSON 으로도 본다.
 */
@XmlRootElement(name = "dailyClose")
@XmlAccessorType(XmlAccessType.FIELD)
public class DailyCloseReport {

    @XmlElement
    private String closeDate;

    @XmlElement
    private String generatedAt;

    @XmlElement
    private String branchCode;

    @XmlElement
    private int accountCount;

    @XmlElement
    private int activeAccountCount;

    @XmlElement
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @XmlElement
    private int transactionCount;

    @XmlElement
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @XmlElement
    private BigDecimal withdrawAmount = BigDecimal.ZERO;

    @XmlElement
    private BigDecimal transferOutAmount = BigDecimal.ZERO;

    @XmlElement
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @XmlElementWrapper(name = "byType")
    @XmlElement(name = "type")
    private List<TypeSummary> byType = new ArrayList<TypeSummary>();

    public String getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(String closeDate) {
        this.closeDate = closeDate;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public int getActiveAccountCount() {
        return activeAccountCount;
    }

    public void setActiveAccountCount(int activeAccountCount) {
        this.activeAccountCount = activeAccountCount;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getWithdrawAmount() {
        return withdrawAmount;
    }

    public void setWithdrawAmount(BigDecimal withdrawAmount) {
        this.withdrawAmount = withdrawAmount;
    }

    public BigDecimal getTransferOutAmount() {
        return transferOutAmount;
    }

    public void setTransferOutAmount(BigDecimal transferOutAmount) {
        this.transferOutAmount = transferOutAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public List<TypeSummary> getByType() {
        return byType;
    }

    public void setByType(List<TypeSummary> byType) {
        this.byType = byType;
    }

    /** 거래 종류별 요약 */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TypeSummary {

        @XmlElement
        private String type;

        @XmlElement
        private String label;

        @XmlElement
        private int count;

        @XmlElement
        private BigDecimal amount = BigDecimal.ZERO;

        public TypeSummary() {
        }

        public TypeSummary(String type, String label) {
            this.type = type;
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
