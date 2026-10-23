package com.novabank.ops.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 수신 계좌.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountNo;
    private String customerName;
    private AccountType type = AccountType.SAVINGS;
    private BigDecimal balance = BigDecimal.ZERO;
    private AccountStatus status = AccountStatus.ACTIVE;
    private Date openedAt;
    private Date closedAt;

    public Account() {
    }

    public Account(String accountNo, String customerName, AccountType type, BigDecimal balance, Date openedAt) {
        this.accountNo = accountNo;
        this.customerName = customerName;
        this.type = type;
        this.balance = balance;
        this.openedAt = openedAt;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Date getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Date openedAt) {
        this.openedAt = openedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /** 화면 표시용 마스킹 이름 (홍길동 → 홍*동) */
    public String getMaskedName() {
        if (customerName == null || customerName.length() < 2) {
            return customerName;
        }
        if (customerName.length() == 2) {
            return customerName.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(customerName.charAt(0));
        for (int i = 1; i < customerName.length() - 1; i++) {
            sb.append('*');
        }
        sb.append(customerName.charAt(customerName.length() - 1));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account other = (Account) o;
        return accountNo != null && accountNo.equals(other.accountNo);
    }

    @Override
    public int hashCode() {
        return accountNo == null ? 0 : accountNo.hashCode();
    }

    @Override
    public String toString() {
        return "Account[" + accountNo + " " + customerName + " " + type + " " + balance + " " + status + "]";
    }
}
