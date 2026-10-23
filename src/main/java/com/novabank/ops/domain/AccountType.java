package com.novabank.ops.domain;

/**
 * 예금 종류.
 */
public enum AccountType {
    SAVINGS("01", "보통예금"),
    CHECKING("02", "당좌예금"),
    FIXED("03", "정기예금");

    private final String code;
    private final String label;

    private AccountType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AccountType fromCode(String code) {
        if (code == null) {
            return SAVINGS;
        }
        for (AccountType t : values()) {
            if (t.code.equals(code.trim()) || t.name().equalsIgnoreCase(code.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException("알 수 없는 예금 종류: " + code);
    }
}
