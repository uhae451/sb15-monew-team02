package com.sprint.mission.sb15monewteam02.common.exception;

import com.sprint.mission.sb15monewteam02.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    log.warn("요청 본문 유효성 검증에 실패했습니다: {}", exception.getBindingResult().getFieldErrors());
    return badRequest(ErrorCode.INVALID_INPUT_VALUE);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {
    log.warn("요청 파라미터 유효성 검증에 실패했습니다: {}", exception.getConstraintViolations());
    return badRequest(ErrorCode.INVALID_INPUT_VALUE);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception) {
    log.warn("요청 본문을 읽을 수 없습니다: {}", exception.getMessage());
    return badRequest(ErrorCode.INVALID_INPUT_VALUE);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
      MissingRequestHeaderException exception) {
    log.warn("필수 요청 헤더가 누락되었습니다: {}", exception.getHeaderName());
    return badRequest(ErrorCode.MISSING_REQUEST_HEADER);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
      MissingServletRequestParameterException exception) {
    log.warn("필수 요청 파라미터가 누락되었습니다: {}", exception.getParameterName());
    return badRequest(ErrorCode.MISSING_REQUEST_PARAMETER);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    log.warn("요청 파라미터 타입이 올바르지 않습니다: {}={}", exception.getName(), exception.getValue());
    return badRequest(ErrorCode.INVALID_TYPE_VALUE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
    log.error("예상하지 못한 예외가 발생했습니다", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
  }

  private ResponseEntity<ErrorResponse> badRequest(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode));
  }
}
