package com.example.demologin.utils;

import java.util.regex.Pattern;

public final class EmailUtils {

    private static final String EMAIL_PATTERN =
            "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    private EmailUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        if (!pattern.matcher(email).matches()) {
            return false;
        }

        if (email.length() > 320) {
            return false;
        }

        return !(email.contains("..") || email.startsWith(".") || email.endsWith("."));
    }

}
