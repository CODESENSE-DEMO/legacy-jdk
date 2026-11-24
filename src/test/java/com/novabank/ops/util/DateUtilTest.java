package com.novabank.ops.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.joda.time.LocalDate;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class DateUtilTest {

    @Test
    public void 포맷_날짜_형식() throws Exception {
        Date now = new Date();
        String formatted = DateUtil.formatDate(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parsed = sdf.parse(formatted);
        assertTrue(DateUtil.isSameDay(now, parsed));
    }

    @Test
    public void 포맷_날짜_시간_형식() throws Exception {
        Date now = new Date();
        String formatted = DateUtil.formatDateTime(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date parsed = sdf.parse(formatted);
        assertEquals(now.getYear(), parsed.getYear());
        assertEquals(now.getMonth(), parsed.getMonth());
        assertEquals(now.getDate(), parsed.getDate());
        assertEquals(now.getHours(), parsed.getHours());
        assertEquals(now.getMinutes(), parsed.getMinutes());
    }

    @Test
    public void 파싱_날짜_형식() throws Exception {
        Date result = DateUtil.parseDate("2024-01-15");
        assertNotNull(result);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2024-01-15", sdf.format(result));
    }

    @Test(expected = IllegalArgumentException.class)
    public void 파싱_잘못된_형식_예외() {
        DateUtil.parseDate("invalid");
    }

    @Test
    public void 파싱_빈문자열_null반환() {
        assertNull(DateUtil.parseDate(""));
        assertNull(DateUtil.parseDate("  "));
    }

    @Test
    public void 파싱_null입력_null반환() {
        assertNull(DateUtil.parseDate(null));
    }

    @Test
    public void 동일_날짜_검증_참() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = sdf.parse("2024-01-15");
        Date date2 = sdf.parse("2024-01-15");
        assertTrue(DateUtil.isSameDay(date1, date2));
    }

    @Test
    public void 동일_날짜_검증_거짓() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = sdf.parse("2024-01-15");
        Date date2 = sdf.parse("2024-01-16");
        assertFalse(DateUtil.isSameDay(date1, date2));
    }

    @Test
    public void 영업시간_검증_참() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date monday10am = sdf.parse("2024-01-15 10:00");
        assertTrue(DateUtil.isBusinessHours(monday10am));
    }

    @Test
    public void 영업시간_검증_거짓_주말() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date saturday = sdf.parse("2024-01-13 10:00");
        assertFalse(DateUtil.isBusinessHours(saturday));
    }

    @Test
    public void 영업시간_검증_거짓_퇴근후() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date monday8pm = sdf.parse("2024-01-15 20:00");
        assertFalse(DateUtil.isBusinessHours(monday8pm));
    }

    @Test
    public void LocalDate_변환() {
        Date result = DateUtil.toDate(new LocalDate(2024, 1, 15));
        assertNotNull(result);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2024-01-15", sdf.format(result));
    }

    @Test
    public void LocalDate_변환_null입력() {
        assertNull(DateUtil.toDate(null));
    }

    @Test
    public void 오늘_날짜() {
        LocalDate result = DateUtil.today();
        assertNotNull(result);
    }
}
