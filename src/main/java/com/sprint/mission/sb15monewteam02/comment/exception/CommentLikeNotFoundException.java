package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

public class CommentLikeNotFoundException extends BusinessException {

  public CommentLikeNotFoundException() {
    super(ErrorCode.COMMENT_LIKE_NOT_FOUND);
  }
}
