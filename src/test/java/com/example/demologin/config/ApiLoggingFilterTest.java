package com.example.demologin.config;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ApiLoggingFilterTest {

    @Test
    void sanitizeQueryString_redacts_googleAccessToken_and_access_token() throws Exception {
        ApiLoggingFilter filter = new ApiLoggingFilter();
        Method m = ApiLoggingFilter.class.getDeclaredMethod("sanitizeQueryString", String.class);
        m.setAccessible(true);

        String qs = "calendarId=primary&timeMin=2023-01-01T00:00:00Z&googleAccessToken=ya29.secret&foo=bar&access_token=abc123";
        String out = (String) m.invoke(filter, qs);

        assertEquals("calendarId=primary&timeMin=2023-01-01T00:00:00Z&googleAccessToken=[REDACTED]&foo=bar&access_token=[REDACTED]", out);
    }

    @Test
    void sanitizeQueryString_preserves_unknown_params() throws Exception {
        ApiLoggingFilter filter = new ApiLoggingFilter();
        Method m = ApiLoggingFilter.class.getDeclaredMethod("sanitizeQueryString", String.class);
        m.setAccessible(true);

        String qs = "a=1&b=2";
        String out = (String) m.invoke(filter, qs);

        assertEquals("a=1&b=2", out);
    }
}
