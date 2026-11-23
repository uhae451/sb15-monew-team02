package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

public class InvalidCursorException extends BusinessException {

  public InvalidCursorException() {
    super(ErrorCode.INVALID_CURSOR);
  }
}
