package com.sprint.mission.sb15monewteam02.common.exception;

import java.util.Objects;

public class DuplicateEmailException extends RuntimeException {
  private final ErrorCode errorCode;

  public DuplicateEmailException(ErrorCode errorCode) {
    super(Objects.requireNonNull(errorCode).getMessage());
    this.errorCode = errorCode;
  }
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}