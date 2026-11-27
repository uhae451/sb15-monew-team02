package com.sprint.mission.sb15monewteam02.common.exception;

// 삭제된 사용자가 있으면 예외처리
public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {

        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
