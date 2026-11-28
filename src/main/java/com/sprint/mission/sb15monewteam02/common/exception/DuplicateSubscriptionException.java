package com.sprint.mission.sb15monewteam02.common.exception;

public class DuplicateSubscriptionException extends BusinessException {
    public DuplicateSubscriptionException() {
        super(ErrorCode.DUPLICATE_SUBSCRIPTION);
    }
}
