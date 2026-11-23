package com.sprint.mission.sb15monewteam02.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String requestId,
        List<FieldErrorDetail> fieldErrors
) {
    public record FieldErrorDetail(
            String field,
            String rejectedValue,
            String reason
    ) {
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String requestId) {
        return new ErrorResponse(
                Instant.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message,
                requestId,
                List.of()
        );
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String message,
            String requestId,
            List<FieldErrorDetail> fieldErrors
    ) {
        return new ErrorResponse(
                Instant.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message,
                requestId,
                fieldErrors
        );
    }
}
