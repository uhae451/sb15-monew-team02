package com.sprint.mission.sb15monewteam02.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("요청 ID 헤더가 없으면 UUID를 생성해 MDC와 응답 헤더에 추가하고 종료 후 MDC를 비운다")
    void generatesRequestIdAndClientIpInMdcAndHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/comments");
        request.setRemoteAddr("192.168.0.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedClientIp = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedRequestId.set(MDC.get(MdcLoggingFilter.MDC_REQUEST_ID_KEY));
            capturedClientIp.set(MDC.get(MdcLoggingFilter.MDC_CLIENT_IP_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedRequestId.get()).isNotBlank();
        assertThat(capturedClientIp.get()).isEqualTo("192.168.0.10");
        assertThat(response.getHeader(MdcLoggingFilter.HEADER_REQUEST_ID)).isEqualTo(capturedRequestId.get());
        assertThat(response.getHeader(MdcLoggingFilter.HEADER_CLIENT_IP)).isEqualTo("192.168.0.10");
        assertThat(MDC.get(MdcLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.MDC_CLIENT_IP_KEY)).isNull();
    }

    @Test
    @DisplayName("X-Request-ID 및 X-Forwarded-For 헤더가 있으면 첫 번째 IP와 기존 요청 ID를 사용한다")
    void usesProvidedRequestIdAndForwardedForIp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/comments");
        request.addHeader(MdcLoggingFilter.HEADER_REQUEST_ID, "req-12345");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedClientIp = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            capturedRequestId.set(MDC.get(MdcLoggingFilter.MDC_REQUEST_ID_KEY));
            capturedClientIp.set(MDC.get(MdcLoggingFilter.MDC_CLIENT_IP_KEY));
        });

        assertThat(capturedRequestId.get()).isEqualTo("req-12345");
        assertThat(capturedClientIp.get()).isEqualTo("203.0.113.5");
        assertThat(response.getHeader(MdcLoggingFilter.HEADER_REQUEST_ID)).isEqualTo("req-12345");
        assertThat(response.getHeader(MdcLoggingFilter.HEADER_CLIENT_IP)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("필터 체인에서 예외가 발생해도 MDC가 반드시 정리된다")
    void clearsMdcEvenWhenChainThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/comments");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class);

        assertThat(MDC.get(MdcLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.MDC_CLIENT_IP_KEY)).isNull();
    }
}
