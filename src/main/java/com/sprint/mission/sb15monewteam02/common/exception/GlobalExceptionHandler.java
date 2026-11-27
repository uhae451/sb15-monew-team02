package com.sprint.mission.sb15monewteam02.common.exception;

import com.sprint.mission.sb15monewteam02.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException exception) {
    log.warn("요청 본문 유효성 검증에 실패했습니다: {}", exception.getBindingResult().getFieldErrors());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {
    log.warn("요청 파라미터 유효성 검증에 실패했습니다: {}", exception.getConstraintViolations());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception) {
    log.warn("요청 본문을 읽을 수 없습니다: {}", exception.getMessage());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
      MissingRequestHeaderException exception) {
    log.warn("필수 요청 헤더가 누락되었습니다: {}", exception.getHeaderName());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.MISSING_REQUEST_HEADER));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
      MissingServletRequestParameterException exception) {
    log.warn("필수 요청 파라미터가 누락되었습니다: {}", exception.getParameterName());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.MISSING_REQUEST_PARAMETER));

  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    log.warn("요청 파라미터 타입이 올바르지 않습니다: {}={}", exception.getName(), exception.getValue());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE));
  }

  // 존재하지 않는 경로 요청은 서버 오류가 아니므로 404 + warn 로그로 처리한다.
  // (없으면 아래 Exception 핸들러가 잡아서 500 + ERROR 로그가 남는다)
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(
      Exception exception, HttpServletRequest request) {
    log.warn("존재하지 않는 경로로 요청했습니다: {} {}", request.getMethod(), request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
    log.error("예상하지 못한 예외가 발생했습니다", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
  }

  // 필수 헤더 누락 및 잘못된 요청 데이터를 400 오류로 처리한다.
  @ExceptionHandler({
      IllegalArgumentException.class,
      DateTimeParseException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {

    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
  }
}
