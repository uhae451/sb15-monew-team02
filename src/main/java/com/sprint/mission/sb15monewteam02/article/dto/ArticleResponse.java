package com.sprint.mission.sb15monewteam02.article.dto;

import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(UUID id, String source, String sourceUrl, String title, Instant publishDate,
    String summary, long commentCount, long viewCount, boolean viewedByMe) {}
