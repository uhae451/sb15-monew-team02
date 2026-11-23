package com.sprint.mission.sb15monewteam02.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("BusinessException 발생 시 해당 상태코드와 ErrorCode 메시지 및 requestId를 반환한다")
    void handlesBusinessException() throws Exception {
        MDC.put("requestId", "req-test-1");
        try {
            mockMvc.perform(get("/test/business"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.requestId").value("req-test-1"));
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("RequestBody 유효성 검증 실패 시 400과 fieldErrors를 반환한다")
    void handlesMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
    }

    @Test
    @DisplayName("필수 요청 헤더 누락 시 400을 반환한다")
    void handlesMissingRequestHeader() throws Exception {
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"));
    }

    @Test
    @DisplayName("타입 변환 실패 시 400을 반환한다")
    void handlesTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type").param("id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TYPE_VALUE"));
    }

    @Test
    @DisplayName("필수 요청 파라미터 누락 시 400을 반환한다")
    void handlesMissingRequestParameter() throws Exception {
        mockMvc.perform(get("/test/type"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUEST_PARAMETER"));
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 400을 반환한다")
    void handlesHttpMessageNotReadable() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("예상하지 못한 예외 발생 시 500을 반환한다")
    void handlesUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @RestController
    @Validated
    static class DummyController {
        @GetMapping("/test/business")
        void throwBusiness() {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "커스텀 메시지");
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody DummyBody body) {
        }

        @GetMapping("/test/header")
        void header(@RequestHeader("Monew-Request-User-ID") UUID userId) {
        }

        @GetMapping("/test/type")
        void type(@RequestParam UUID id) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("unexpected error");
        }
    }

    record DummyBody(@NotBlank String content) {
    }
}
