package com.novabank.ops.domain;

/**
 * 거래 종류. 입금성(+) / 출금성(-).
 */
public enum TxType {
    DEPOSIT("입금", true),
    WITHDRAW("출금", false),
    TRANSFER_IN("이체입금", true),
    TRANSFER_OUT("이체출금", false),
    FEE("수수료", false);

    private final String label;
    private final boolean credit;

    private TxType(String label, boolean credit) {
        this.label = label;
        this.credit = credit;
    }

    public String getLabel() {
        return label;
    }

    /** 잔액이 늘어나는 거래인가 */
    public boolean isCredit() {
        return credit;
    }
}
