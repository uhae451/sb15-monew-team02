package com.sprint.mission.sb15monewteam02.common.exception;

import com.sprint.mission.sb15monewteam02.common.logging.MdcLoggingFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        return buildResponse(errorCode, ex.getMessage(), List.of());
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
        return buildResponse(errorCode, errorCode.getMessage(), fieldErrors);
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
        return buildResponse(errorCode, errorCode.getMessage(), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        log.warn("Unreadable HTTP message: {}", ex.getMessage());
        return buildResponse(errorCode, "요청 본문 형식이 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_HEADER;
        String message = String.format("필수 요청 헤더(%s)가 누락되었습니다.", ex.getHeaderName());
        log.warn("Missing request header: {}", ex.getHeaderName());
        return buildResponse(errorCode, message, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
        String message = String.format("필수 요청 파라미터(%s)가 누락되었습니다.", ex.getParameterName());
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return buildResponse(errorCode, message, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_TYPE_VALUE;
        String message = String.format("요청 파라미터(%s)의 형식이 올바르지 않습니다.", ex.getName());
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());
        return buildResponse(errorCode, message, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception occurred", ex);
        return buildResponse(errorCode, errorCode.getMessage(), List.of());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode errorCode,
            String message,
            List<ErrorResponse.FieldErrorDetail> fieldErrors
    ) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message, MDC.get(MdcLoggingFilter.MDC_REQUEST_ID_KEY), fieldErrors));
    }
}
