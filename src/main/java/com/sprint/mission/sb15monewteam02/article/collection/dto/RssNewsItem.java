package com.sprint.mission.sb15monewteam02.article.collection.dto;

import java.time.Instant;

public record RssNewsItem(
    String source,
    String title,
    String link,
    String description,
    Instant pubDate
) {
}