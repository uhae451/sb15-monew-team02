package com.sprint.mission.sb15monewteam02.common.exception;

public class InterestKeywordEmptyException extends BusinessException {

    public InterestKeywordEmptyException() {
        super(ErrorCode.INTEREST_KEYWORD_EMPTY);
    }
}
