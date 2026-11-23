package com.sprint.mission.sb15monewteam02.comment.port;

import java.util.Optional;
import java.util.UUID;

public interface ArticleReader {

    Optional<ArticleSummary> findActiveArticle(UUID articleId);
}
