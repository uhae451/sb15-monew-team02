package com.sprint.mission.sb15monewteam02.article.dto;

import java.util.List;

public record ArticlePage(List<ArticleResponse> content, String nextCursor, String nextAfter,
    int size, long totalElements, boolean hasNext) {}
