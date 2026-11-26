package com.sprint.mission.sb15monewteam02.common.exception;

import java.util.Objects;

public class DuplicateEmailException extends BusinessException {
  public DuplicateEmailException() {
    super(ErrorCode.DUPLICATE_EMAIL);
  }
}