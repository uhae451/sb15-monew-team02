package com.sprint.mission.sb15monewteam02.common.exception;

public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {

        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
