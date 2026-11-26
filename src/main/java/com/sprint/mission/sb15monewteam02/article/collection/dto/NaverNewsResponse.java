package com.sprint.mission.sb15monewteam02.article.collection.dto;

import java.util.List;

public record NaverNewsResponse(
    String lastBuildDate,
    int total,
    int start,
    int display,
    List<NaverNewsItem> items
) {
}