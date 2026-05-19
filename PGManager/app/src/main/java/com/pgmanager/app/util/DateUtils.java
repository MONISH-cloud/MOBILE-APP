package com.pgmanager.app.util;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return dateFormat.format(timestamp.toDate());
    }

    public static String getCurrentMonth() {
        return monthFormat.format(new Date());
    }

    public static String formatMonth(Date date) {
        return monthFormat.format(date);
    }

    public static Timestamp getStartOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return new Timestamp(cal.getTime());
    }

    public static Timestamp getEndOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return new Timestamp(cal.getTime());
    }

    public static long getDaysBetween(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return diff / (24 * 60 * 60 * 1000);
    }
}
