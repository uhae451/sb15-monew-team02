package com.sprint.mission.sb15monewteam02.common.exception;

public class InterestNameSimilarException extends RuntimeException {
    public InterestNameSimilarException() {
        super(String.valueOf(ErrorCode.INTEREST_NAME_SIMILAR));
    }
}
