package com.sprint.mission.sb15monewteam02.common.exception;

public class InterestNotFoundException extends BusinessException {
    public InterestNotFoundException() {
        super(ErrorCode.INTEREST_NOT_FOUND);
    }
}
