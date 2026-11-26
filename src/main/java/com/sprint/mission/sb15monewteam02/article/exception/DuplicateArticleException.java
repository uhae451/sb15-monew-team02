package com.sprint.mission.sb15monewteam02.article.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

// 이미 등록된 기사를 중복 등록할 때 발생하는 예외
public class DuplicateArticleException extends BusinessException {

  public DuplicateArticleException() {
    super(ErrorCode.DUPLICATE_ARTICLE);
  }
}