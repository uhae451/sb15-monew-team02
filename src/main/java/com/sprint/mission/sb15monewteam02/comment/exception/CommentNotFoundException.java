package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

import java.util.UUID;

public class CommentNotFoundException extends BusinessException {

    public CommentNotFoundException(UUID commentId) {
        super(ErrorCode.COMMENT_NOT_FOUND, "댓글을 찾을 수 없습니다: " + commentId);
    }
}
