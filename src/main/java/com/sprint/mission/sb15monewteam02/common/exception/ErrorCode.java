package com.sprint.mission.sb15monewteam02.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 예시
  //   MEMBER_NOT_FOUND("MEM_001","회원을 찾을 수 없습니다.",HttpStatus.NOT_FOUND)

  INTERNAL_ERROR("CMN_999","서버에 문제가 발생했습니다",HttpStatus.INTERNAL_SERVER_ERROR)
  ;

  private final String code;

  private final String message;

  private final HttpStatus httpStatus;
}
