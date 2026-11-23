package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

public class DuplicateCommentLikeException extends BusinessException {

  public DuplicateCommentLikeException() {
    super(ErrorCode.DUPLICATE_COMMENT_LIKE);
  }
}
