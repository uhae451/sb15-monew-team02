package com.sprint.mission.sb15monewteam02.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_CLIENT_IP = "X-Client-IP";
    public static final String MDC_REQUEST_ID_KEY = "requestId";
    public static final String MDC_CLIENT_IP_KEY = "clientIp";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        String clientIp = resolveClientIp(request);

        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        MDC.put(MDC_CLIENT_IP_KEY, clientIp);

        response.setHeader(HEADER_REQUEST_ID, requestId);
        response.setHeader(HEADER_CLIENT_IP, clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(HEADER_REQUEST_ID);
        return StringUtils.hasText(header) ? header.trim() : UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
