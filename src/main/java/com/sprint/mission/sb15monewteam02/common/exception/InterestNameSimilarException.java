package com.sprint.mission.sb15monewteam02.common.exception;

public class InterestNameSimilarException extends BusinessException {
    public InterestNameSimilarException() {
        super(ErrorCode.INTEREST_NAME_SIMILAR);
    }
}
