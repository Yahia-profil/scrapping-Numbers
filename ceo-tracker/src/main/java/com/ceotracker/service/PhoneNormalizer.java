package com.ceotracker.service;

import java.util.regex.Pattern;

public class PhoneNormalizer {

    private static final Pattern MOROCCAN_PHONE = Pattern.compile(
        "^(?:(?:\\+|00)212|0)[5-7]\\d{8}$"
    );

    private static final Pattern RAW_DIGITS = Pattern.compile("\\d+");

    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^\\d+]", "");
        if (digits.startsWith("+")) {
            digits = "00" + digits.substring(1);
        }
        if (digits.startsWith("00212")) {
            digits = "0" + digits.substring(5);
        } else if (digits.startsWith("212") && digits.length() == 12) {
            digits = "0" + digits.substring(3);
        }
        if (digits.length() == 10 && digits.startsWith("0")) {
            return digits;
        }
        return null;
    }

    public static boolean isValidFormat(String phone) {
        if (phone == null) return false;
        return MOROCCAN_PHONE.matcher(phone).matches();
    }
}
