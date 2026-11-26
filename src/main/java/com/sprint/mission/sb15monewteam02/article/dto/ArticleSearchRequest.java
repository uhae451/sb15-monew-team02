package com.sprint.mission.sb15monewteam02.article.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort.Direction;

public record ArticleSearchRequest(
    String keyword, UUID interestId, Set<String> sourceIn,
    Instant publishDateFrom, Instant publishDateTo,
    ArticleOrder orderBy, Direction direction, String cursor, String after, int limit) {
  public ArticleSearchRequest {
    if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
    orderBy = orderBy == null ? ArticleOrder.publishDate : orderBy;
    direction = direction == null ? Direction.DESC : direction;
    sourceIn = sourceIn == null ? Set.of() : Set.copyOf(sourceIn);
    if (publishDateFrom != null && publishDateTo != null && publishDateFrom.isAfter(publishDateTo)) {
      throw new IllegalArgumentException("publishDateFrom must not be after publishDateTo");
    }
    if ((cursor == null) != (after == null)) throw new IllegalArgumentException("cursor and after must be supplied together");
    if (cursor != null) {
      try {
        UUID parsed = UUID.fromString(after);
        if (!parsed.toString().equalsIgnoreCase(after)) throw new IllegalArgumentException("Invalid after ID");
        if (orderBy == ArticleOrder.publishDate) Instant.parse(cursor);
        else if (Long.parseLong(cursor) < 0) throw new IllegalArgumentException("cursor count must be nonnegative");
      } catch (RuntimeException invalid) {
        throw new IllegalArgumentException("Invalid article cursor", invalid);
      }
    }
  }
}
