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

  // 관심사 예외
  INTEREST_KEYWORD_EMPTY("ITR_001", "키워드는 1개 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
  INTEREST_NAME_BLANK("ITR_002", "관심사 이름은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
  INTEREST_NAME_SIMILAR("ITR_003","유사한 이름의 관심사가 이미 존재합니다.", HttpStatus.BAD_REQUEST),
  INTEREST_NOT_FOUND("ITR_004", "관심사를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
 
  DUPLICATE_EMAIL("USR_001","이미 가입된 이메일입니다.",HttpStatus.CONFLICT),
  INVALID_INPUT("CMN_400","입력값이 올바르지 않습니다.",HttpStatus.BAD_REQUEST),
  INVALID_CREDENTIALS("USR_002", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
  
  // 공통(CMN)
  INVALID_TYPE_VALUE("CMN_002", "요청 파라미터 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
  MISSING_REQUEST_HEADER("CMN_003", "필수 요청 헤더가 누락되었습니다.", HttpStatus.BAD_REQUEST),
  MISSING_REQUEST_PARAMETER("CMN_004", "필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
  INVALID_CURSOR("CMN_005", "유효하지 않은 커서 형식입니다.", HttpStatus.BAD_REQUEST),

  // 사용자(USR)
  USER_NOT_FOUND("USR_003", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  // 댓글(CMT)
  COMMENT_NOT_FOUND("CMT_001", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  COMMENT_FORBIDDEN("CMT_002", "본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
  DUPLICATE_COMMENT_LIKE("CMT_003", "이미 좋아요를 누른 댓글입니다.", HttpStatus.CONFLICT),
  COMMENT_LIKE_NOT_FOUND("CMT_004", "좋아요 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  // 기사 관련 예외 코드
  ARTICLE_NOT_FOUND("ART_001", "기사를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  DUPLICATE_ARTICLE("ART_002", "이미 등록된 기사입니다.", HttpStatus.CONFLICT),
  ;



  private final String code;

  private final String message;

  private final HttpStatus httpStatus;
}
