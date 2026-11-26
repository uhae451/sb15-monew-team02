package com.sprint.mission.sb15monewteam02.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 예시
  //   MEMBER_NOT_FOUND("MEM_001","회원을 찾을 수 없습니다.",HttpStatus.NOT_FOUND)

  INTERNAL_ERROR("CMN_999","서버에 문제가 발생했습니다",HttpStatus.INTERNAL_SERVER_ERROR),
  DUPLICATE_EMAIL("USR_001","이미 가입된 이메일입니다.",HttpStatus.CONFLICT),
  INVALID_INPUT("CMN_400","입력값이 올바르지 않습니다.",HttpStatus.BAD_REQUEST),
  INVALID_CREDENTIALS("USR_002", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
  USER_NOT_FOUND("USR_003", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ;



  private final String code;

  private final String message;

  private final HttpStatus httpStatus;
}
