package com.novabank.ops.domain;

/**
 * 계좌 상태.
 */
public enum AccountStatus {
    ACTIVE("정상"),
    FROZEN("거래정지"),
    CLOSED("해지");

    private final String label;

    private AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AccountStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("계좌 상태 코드가 없습니다.");
        }
        // JDK 7 부터 가능한 switch on String
        switch (code.trim().toUpperCase()) {
            case "ACTIVE":
            case "A":
                return ACTIVE;
            case "FROZEN":
            case "F":
                return FROZEN;
            case "CLOSED":
            case "C":
                return CLOSED;
            default:
                throw new IllegalArgumentException("알 수 없는 계좌 상태 코드: " + code);
        }
    }
}
