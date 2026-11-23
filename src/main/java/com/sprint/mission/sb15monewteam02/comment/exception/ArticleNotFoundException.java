package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

public class ArticleNotFoundException extends BusinessException {

  public ArticleNotFoundException() {
    super(ErrorCode.ARTICLE_NOT_FOUND);
  }
}
