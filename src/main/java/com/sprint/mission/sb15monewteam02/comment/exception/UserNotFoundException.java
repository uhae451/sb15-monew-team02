package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

import java.util.UUID;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(UUID userId) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId);
    }
}
