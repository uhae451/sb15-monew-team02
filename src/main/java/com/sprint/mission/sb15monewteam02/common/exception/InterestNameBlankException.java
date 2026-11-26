package com.sprint.mission.sb15monewteam02.common.exception;

public class InterestNameBlankException extends BusinessException {

    public InterestNameBlankException() {
        super(ErrorCode.INTEREST_NAME_BLANK);
    }
}