package com.sprint.mission.sb15monewteam02.common.dto;

import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

public record ErrorResponse(String code, String message) {
  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
  }
}
