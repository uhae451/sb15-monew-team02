package com.sprint.mission.sb15monewteam02.common.exception;

public class SubscriptionNotFoundException extends BusinessException {
    public SubscriptionNotFoundException() {
        super(ErrorCode.SUBSCRIPTION_NOT_FOUND);
    }
}
