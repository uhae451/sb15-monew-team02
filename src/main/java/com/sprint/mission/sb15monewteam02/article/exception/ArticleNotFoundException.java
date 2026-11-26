package com.sprint.mission.sb15monewteam02.article.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;
import java.util.UUID;

// 존재하지 않는 기사를 조회할 때 발생하는 예외
public class ArticleNotFoundException extends BusinessException {

  public ArticleNotFoundException(UUID id) {
    super(ErrorCode.ARTICLE_NOT_FOUND);
  }
}