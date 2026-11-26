package com.sprint.mission.sb15monewteam02.article.dto;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewResponse(
    UUID id,
    UUID viewedBy,
    Instant createdAt,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    long articleCommentCount,
    long articleViewCount
) {
}