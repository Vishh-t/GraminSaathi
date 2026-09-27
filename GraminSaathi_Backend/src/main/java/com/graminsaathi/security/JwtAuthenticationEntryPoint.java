package com.graminsaathi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Security's default behavior for an unauthenticated request to a
 * protected endpoint is an empty-body 403 Forbidden - indistinguishable from
 * a real authorization failure. That meant an expired/missing/invalid JWT on
 * /api/reports looked identical to a genuine permissions error, and the
 * frontend's "on 401, clear token + redirect to /login" logic never fired.
 * This entry point makes "not authenticated" correctly return 401 with a
 * clear message, wired in via SecurityConfig's .exceptionHandling(...).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "Your session has expired or is invalid. Please log in again.");

        objectMapper.writeValue(response.getWriter(), body);
    }
}
