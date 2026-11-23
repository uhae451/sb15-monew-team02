package com.sprint.mission.sb15monewteam02.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_TYPE_VALUE", "요청 파라미터 타입이 올바르지 않습니다."),
    MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_HEADER", "필수 요청 헤더가 누락되었습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_PARAMETER", "필수 요청 파라미터가 누락되었습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "유효하지 않은 커서 형식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // User & Article
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "기사를 찾을 수 없습니다."),

    // Comment & CommentLike
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다."),
    DUPLICATE_COMMENT_LIKE(HttpStatus.CONFLICT, "DUPLICATE_COMMENT_LIKE", "이미 좋아요를 누른 댓글입니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_LIKE_NOT_FOUND", "좋아요 내역을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
