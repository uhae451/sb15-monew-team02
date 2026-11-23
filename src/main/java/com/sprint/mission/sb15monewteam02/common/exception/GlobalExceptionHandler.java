package com.sprint.mission.sb15monewteam02.common.exception;

import com.sprint.mission.sb15monewteam02.common.logging.MdcLoggingFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Business exception occurred: code={}, message={}", errorCode.getCode(), ex.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, ex.getMessage(), currentRequestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldErrorDetail(
                        error.getField(),
                        error.getRejectedValue() != null ? error.getRejectedValue().toString() : null,
                        error.getDefaultMessage()
                ))
                .toList();

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentRequestId(), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : null,
                        violation.getMessage()
                ))
                .toList();

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        log.warn("Constraint violation: {}", fieldErrors);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentRequestId(), fieldErrors));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_HEADER;
        String message = String.format("필수 요청 헤더(%s)가 누락되었습니다.", ex.getHeaderName());
        log.warn("Missing request header: {}", ex.getHeaderName());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message, currentRequestId()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
        String message = String.format("필수 요청 파라미터(%s)가 누락되었습니다.", ex.getParameterName());
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message, currentRequestId()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_TYPE_VALUE;
        String message = String.format("요청 파라미터(%s)의 형식이 올바르지 않습니다.", ex.getName());
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message, currentRequestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception occurred", ex);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentRequestId()));
    }

    private String currentRequestId() {
        return MDC.get(MdcLoggingFilter.MDC_REQUEST_ID_KEY);
    }
}
