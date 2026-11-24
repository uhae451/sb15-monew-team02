package com.sprint.mission.sb15monewteam02;

import com.sprint.mission.sb15monewteam02.article.dto.*;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;
import static org.junit.jupiter.api.Assertions.*;

class ArticleSearchRequestTest {
  @Test
  void rejectsInvalidLimit() {
    assertThrows(IllegalArgumentException.class, () -> request(null, null, 0));
    assertThrows(IllegalArgumentException.class, () -> request(null, null, 101));
  }
  @Test
  void rejectsIncompleteOrMalformedCursor() {
    assertThrows(IllegalArgumentException.class, () -> request("1", null, 10));
    assertThrows(IllegalArgumentException.class, () -> request(null, "id", 10));
    assertThrows(IllegalArgumentException.class, () -> request("not-a-number", java.util.UUID.randomUUID().toString(), 10));
    assertThrows(IllegalArgumentException.class, () -> request("-1", java.util.UUID.randomUUID().toString(), 10));
    assertThrows(IllegalArgumentException.class, () -> request("1", "bad-id", 10));
  }
  @Test
  void rejectsReversedDates() {
    assertThrows(IllegalArgumentException.class, () -> new ArticleSearchRequest(
        null, null, Set.of(), Instant.parse("2026-11-21T00:00:00Z"), Instant.parse("2026-11-20T00:00:00Z"),
        ArticleOrder.publishDate, Direction.DESC, null, null, 10));
  }
  @Test
  void defaultsSortingAndCopiesSources() {
    var sources = new java.util.HashSet<>(Set.of("NAVER"));
    var request = new ArticleSearchRequest(null, null, sources, null, null, null, null, null, null, 10);
    sources.clear();
    assertEquals(ArticleOrder.publishDate, request.orderBy());
    assertEquals(Direction.DESC, request.direction());
    assertEquals(Set.of("NAVER"), request.sourceIn());
  }
  private ArticleSearchRequest request(String cursor, String after, int limit) {
    return new ArticleSearchRequest(null, null, Set.of(), null, null, ArticleOrder.viewCount, Direction.ASC, cursor, after, limit);
  }
}
