package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

public class InvalidCursorException extends BusinessException {

    public InvalidCursorException(String cursorValue) {
        super(ErrorCode.INVALID_CURSOR, "유효하지 않은 커서 형식입니다: " + cursorValue);
    }
}
