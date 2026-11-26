package com.sprint.mission.sb15monewteam02.article.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArticleEntityTest {
  @Test
  void publishDateUsesPostgresMicrosecondPrecision() {
    Instant input = Instant.parse("2026-11-20T10:00:00.123456789Z");
    Article article = new Article("NAVER", "https://example.com/precision", "Java", input, "Spring");
    assertEquals(Instant.parse("2026-11-20T10:00:00.123456Z"), article.getPublishDate());
  }
  @Test
  void softDeleteIsIdempotent() {
    Article article = new Article("NAVER", "https://example.com/delete", "Java", Instant.now(), "Spring");
    article.softDelete();
    article.softDelete();
    assertTrue(article.isDeleted());
  }

  @Test
  void recordsOneView() {
    Article article = new Article("NAVER", "https://example.com/view", "Java", Instant.now(), "Spring");
    article.recordView();
    assertEquals(1, article.getViewCount());
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.NullAndEmptySource
  @org.junit.jupiter.params.provider.ValueSource(strings = {" ", "ftp://news.example.com/1", "not-a-url"})
  void rejectsInvalidSourceUrl(String url) {
    assertThrows(IllegalArgumentException.class,
        () -> new Article("NAVER", url, "Java", Instant.now(), "Spring"));
  }

  @Test
  void rejectsMissingRequiredInformation() {
    Instant now = Instant.now();
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new Article(" ", "https://example.com", "title", now, "summary")),
        () -> assertThrows(IllegalArgumentException.class, () -> new Article("NAVER", "https://example.com", "", now, "summary")),
        () -> assertThrows(IllegalArgumentException.class, () -> new Article("NAVER", "https://example.com", "title", null, "summary")),
        () -> assertThrows(IllegalArgumentException.class, () -> new Article("NAVER", "https://example.com", "title", now, null)));
  }

  @Test
  void createsArticleWithRequiredInformation() {
    // given
    Instant published = Instant.parse("2026-11-20T10:00:00Z");
    // when
    Article article = new Article("NAVER", "https://news.example.com/1", "Java", published, "Spring");
    // then
    assertAll(
        () -> assertEquals("NAVER", article.getSource()),
        () -> assertEquals("https://news.example.com/1", article.getSourceUrl()),
        () -> assertEquals("Java", article.getTitle()),
        () -> assertEquals(published, article.getPublishDate()),
        () -> assertEquals("Spring", article.getSummary()),
        () -> assertEquals(0, article.getViewCount()),
        () -> assertFalse(article.isDeleted()));
  }
}
