package com.sprint.mission.sb15monewteam02.comment.exception;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;

import java.util.UUID;

public class ArticleNotFoundException extends BusinessException {

    public ArticleNotFoundException(UUID articleId) {
        super(ErrorCode.ARTICLE_NOT_FOUND, "기사를 찾을 수 없습니다: " + articleId);
    }
}
