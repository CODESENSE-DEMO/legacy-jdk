package com.novabank.ops.domain;

import static org.junit.Assert.*;

import org.junit.Test;

public class AccountStatusTest {

    // === 정상 코드 매핑 ===

    @Test
    public void testFromCode_ACTIVE() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode("ACTIVE"));
    }

    @Test
    public void testFromCode_FROZEN() {
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode("FROZEN"));
    }

    @Test
    public void testFromCode_CLOSED() {
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode("CLOSED"));
    }

    @Test
    public void testFromCode_A() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode("A"));
    }

    @Test
    public void testFromCode_F() {
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode("F"));
    }

    @Test
    public void testFromCode_C() {
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode("C"));
    }

    // === 소문자 ===

    @Test
    public void testFromCode_lowercase_active() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode("active"));
    }

    @Test
    public void testFromCode_lowercase_frozen() {
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode("frozen"));
    }

    @Test
    public void testFromCode_lowercase_closed() {
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode("closed"));
    }

    @Test
    public void testFromCode_lowercase_a() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode("a"));
    }

    @Test
    public void testFromCode_lowercase_f() {
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode("f"));
    }

    @Test
    public void testFromCode_lowercase_c() {
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode("c"));
    }

    // === 앞뒤 공백 ===

    @Test
    public void testFromCode_trimmed_active() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode(" ACTIVE "));
    }

    @Test
    public void testFromCode_trimmed_frozen() {
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode(" FROZEN "));
    }

    @Test
    public void testFromCode_trimmed_closed() {
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode(" CLOSED "));
    }

    @Test
    public void testFromCode_trimmed_a() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode(" A "));
    }

    @Test
    public void testFromCode_trimmed_f() {
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode(" F "));
    }

    @Test
    public void testFromCode_trimmed_c() {
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode(" C "));
    }

    // === null ===

    @Test(expected = IllegalArgumentException.class)
    public void testFromCode_null() {
        AccountStatus.fromCode(null);
    }

    // === 알 수 없는 코드 ===

    @Test(expected = IllegalArgumentException.class)
    public void testFromCode_unknown() {
        AccountStatus.fromCode("X");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromCode_empty() {
        AccountStatus.fromCode("");
    }
}
