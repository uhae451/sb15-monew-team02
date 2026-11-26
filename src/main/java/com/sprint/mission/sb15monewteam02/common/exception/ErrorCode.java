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


  // 관심사 예외
  INTEREST_KEYWORD_EMPTY("ITR_001", "키워드는 1개 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
  INTEREST_NAME_BLANK("ITR_002", "관심사 이름은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
  INTEREST_NAME_SIMILAR("ITR_003","유사한 이름의 관심사가 이미 존재합니다.", HttpStatus.BAD_REQUEST),
  INTEREST_NOT_FOUND("ITR_004", "관심사를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ;

  private final String code;

  private final String message;

  private final HttpStatus httpStatus;
}
