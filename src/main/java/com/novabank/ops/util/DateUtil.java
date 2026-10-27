package com.novabank.ops.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 날짜 유틸. 화면·API 에서 쓰는 날짜 문자열은 모두 yyyy-MM-dd 다.
 */
public final class DateUtil {

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm";

    // 2013-12 성능 이슈로 포맷터를 캐시했다 (매 호출마다 new 하지 않도록)
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat(DATETIME_PATTERN);

    private DateUtil() {
    }

    public static String formatDate(Date date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        return date == null ? "" : DATETIME_FORMAT.format(date);
    }

    public static Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(text.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다(yyyy-MM-dd): " + text, e);
        }
    }

    /** 두 날짜가 같은 날(연·월·일)인가 */
    public static boolean isSameDay(Date a, Date b) {
        if (a == null || b == null) {
            return false;
        }
        Calendar ca = Calendar.getInstance();
        Calendar cb = Calendar.getInstance();
        ca.setTime(a);
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    /** 영업시간(평일 09:00~18:00)인가 */
    public static boolean isBusinessHours(Date at) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(at);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
            return false;
        }
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 9 && hour < 18;
    }

    public static Date toDate(LocalDate localDate) {
        return localDate == null ? null : java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate today() {
        return LocalDate.now();
    }
}
