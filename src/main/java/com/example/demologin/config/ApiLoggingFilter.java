package com.example.demologin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ApiLoggingFilter.class);

    // ANSI colors
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String ORANGE = "\u001B[38;5;208m"; // cam ANSI 256-color
    private static final String BOLD = "\u001B[1m";

    private String colorStatus(int status) {
        if (status >= 200 && status < 300) return BOLD + GREEN;
        if (status >= 300 && status < 400) return BOLD + BLUE;
        if (status >= 400 && status < 500) return BOLD + YELLOW;
        return BOLD + RED;
    }

    private String colorMethod(String method) {
        return switch (method) {
            case "GET" -> BOLD + BLUE;
            case "POST" -> BOLD + GREEN;
            case "PUT" -> BOLD + YELLOW;
            case "DELETE" -> BOLD + RED;
            default -> BOLD + RESET;
        };
    }

    private String colorDuration(long ms) {
        if (ms < 200) return BOLD + GREEN;   // Rất nhanh
        if (ms < 500) return BOLD + BLUE;    // Nhanh
        if (ms < 1000) return BOLD + YELLOW; // Trung bình
        if (ms < 3000) return BOLD + ORANGE; // Hơi chậm
        return BOLD + RED;                   // Rất chậm
    }

    private String speedIcon(long ms) {
        if (ms < 200) return "⚡";
        if (ms < 500) return "🚀";
        if (ms < 1000) return "⏱️";
        if (ms < 3000) return "🐢";
        return "🛑";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIP = request.getRemoteAddr();
        String queryString = request.getQueryString();
        long start = System.currentTimeMillis();

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        String method = request.getMethod();
        int status = response.getStatus();
        String uri = request.getRequestURI() + (queryString == null ? "" : "?" + queryString);

        logger.info("{}{}{} {} {}{}{} from {} took {}{}{} ms {}",
                colorMethod(method), method, RESET,
                uri,
                colorStatus(status), status, RESET,
                clientIP,
                colorDuration(duration), duration, RESET,
                speedIcon(duration)
        );
    }
}
