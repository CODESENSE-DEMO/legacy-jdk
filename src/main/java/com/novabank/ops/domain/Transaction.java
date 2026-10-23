package com.novabank.ops.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 거래 내역 한 줄.
 */
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 저장소가 매기는 일련번호 — 같은 시각 거래의 순서 보장 */
    private long seq;
    private String txId;
    private String accountNo;
    private TxType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String counterparty;
    private String memo;
    private Date createdAt;

    public Transaction() {
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public TxType getType() {
        return type;
    }

    public void setType(TxType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Tx[" + txId + " " + accountNo + " " + type + " " + amount + "]";
    }
}
