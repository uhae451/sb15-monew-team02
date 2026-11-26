package com.sprint.mission.sb15monewteam02.article.repository;

import com.sprint.mission.sb15monewteam02.article.entity.Article;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.jupiter.api.Assertions.*;
import com.sprint.mission.sb15monewteam02.article.dto.*;
import java.util.*;
import org.springframework.data.domain.Sort.Direction;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArticleRepositoryTest {
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"source", "title", "publish_date", "summary"})
  void databaseRejectsMissingRequiredColumn(String column) {
    Article saved = repository.saveAndFlush(article("https://example.com/not-null"));
    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
        "update article_test.articles set " + column + " = null where id = ?", saved.getId()));
  }
  ArticleSearchRequest search(String keyword, UUID interest, Set<String> sources, Instant from, Instant to,
      ArticleOrder order, Direction direction, String cursor, String after, int limit) {
    return new ArticleSearchRequest(keyword, interest, sources, from, to, order, direction, cursor, after, limit);
  }

  @Test
  void searchesTitleAndSummaryIgnoringCaseAndExcludesDeleted() {
    Article titleMatch = repository.save(new Article("NAVER", "https://example.com/title", "Java release", Instant.now(), "language"));
    Article summaryMatch = repository.save(new Article("RSS", "https://example.com/summary", "News", Instant.now(), "JAVA update"));
    repository.save(new Article("RSS", "https://example.com/other", "Other", Instant.now(), "Other"));
    Article deleted = article("https://example.com/deleted");
    deleted.softDelete();
    repository.saveAndFlush(deleted);
    entityManager.clear();
    var result = repository.search(search("java", null, Set.of(), null, null, ArticleOrder.publishDate, Direction.DESC, null, null, 20));
    assertEquals(Set.of(titleMatch.getId(), summaryMatch.getId()), new HashSet<>(result.content().stream().map(Article::getId).toList()));
    assertEquals(2, result.totalElements());
    assertTrue(repository.findByIdAndDeletedFalse(deleted.getId()).isEmpty());
    assertTrue(repository.existsBySourceUrl(deleted.getSourceUrl()));
  }

  @Test
  void combinesInterestSourceAndInclusiveDateFilters() {
    UUID interest = UUID.randomUUID();
    Instant boundary = Instant.parse("2026-11-20T10:00:00Z");
    Article matching = article("https://example.com/matching");
    matching.replaceInterests(Set.of(interest));
    repository.save(matching);
    Article wrongSource = new Article("RSS", "https://example.com/rss", "Java", boundary, "Spring");
    wrongSource.replaceInterests(Set.of(interest));
    repository.save(wrongSource);
    Article wrongDate = new Article("NAVER", "https://example.com/date", "Java", boundary.minusSeconds(1), "Spring");
    wrongDate.replaceInterests(Set.of(interest));
    repository.save(wrongDate);
    repository.saveAndFlush(article("https://example.com/no-interest"));
    entityManager.clear();
    var result = repository.search(search(null, interest, Set.of("NAVER"), boundary, boundary, ArticleOrder.publishDate, Direction.ASC, null, null, 10));
    assertEquals(List.of(matching.getId()), result.content().stream().map(Article::getId).toList());
    assertEquals(Set.of(interest), result.content().get(0).getInterestIds());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(ArticleOrder.class)
  void cursorTraversesTiesWithoutDuplicatesInBothDirections(ArticleOrder order) {
    for (int i = 0; i < 5; i++) repository.save(article("https://example.com/tie/" + i));
    repository.flush();
    entityManager.clear();
    for (Direction direction : Direction.values()) {
      Set<UUID> seen = new HashSet<>();
      String cursor = null;
      String after = null;
      int pages = 0;
      do {
        var result = repository.search(search(null, null, Set.of(), null, null, order, direction, cursor, after, 2));
        assertEquals(5, result.totalElements());
        assertFalse(result.content().isEmpty());
        for (Article row : result.content()) assertTrue(seen.add(row.getId()), "duplicate across pages");
        pages++;
        assertTrue(pages <= 3);
        if (!result.hasNext()) break;
        Article last = result.content().get(result.content().size() - 1);
        cursor = switch (order) {
          case publishDate -> last.getPublishDate().toString();
          case viewCount -> Long.toString(last.getViewCount());
          case commentCount -> Long.toString(last.getCommentCount());
        };
        after = last.getId().toString();
      } while (true);
      assertEquals(5, seen.size());
    }
  }

  @Test
  void sortsByCountsAndPublishDate() {
    Article low = article("https://example.com/low");
    Article high = new Article("NAVER", "https://example.com/high", "Java", Instant.parse("2026-11-21T10:00:00Z"), "Spring");
    high.recordView();
    high.updateCommentCount(3);
    repository.save(low);
    repository.saveAndFlush(high);
    entityManager.clear();
    for (ArticleOrder order : ArticleOrder.values()) {
      var result = repository.search(search(null, null, Set.of(), null, null, order, Direction.DESC, null, null, 10));
      assertEquals(List.of(high.getId(), low.getId()), result.content().stream().map(Article::getId).toList());
    }
  }

  @Test
  void keywordWildcardsAreTreatedLiterally() {
    Article literal = repository.save(new Article("NAVER", "https://example.com/literal", "100%_done", Instant.now(), "text"));
    repository.saveAndFlush(article("https://example.com/unrelated"));
    var result = repository.search(search("%_", null, Set.of(), null, null, ArticleOrder.publishDate, Direction.DESC, null, null, 10));
    assertEquals(List.of(literal.getId()), result.content().stream().map(Article::getId).toList());
  }
  @Autowired ArticleRepository repository;
  @Autowired EntityManager entityManager;

  Article article(String url) {
    return new Article("NAVER", url, "Java", Instant.parse("2026-11-20T10:00:00Z"), "Spring");
  }

  @Test
  void savesAndReloadsArticleFromPostgres() {
    // given / when
    Article saved = repository.saveAndFlush(article("https://example.com/1"));
    entityManager.clear();
    // then: reload after clearing the persistence context
    Article loaded = repository.findById(saved.getId()).orElseThrow();
    assertEquals("https://example.com/1", loaded.getSourceUrl());
    assertTrue(repository.existsBySourceUrl(loaded.getSourceUrl()));
  }

  @Test
  void databaseRejectsDuplicateUrl() {
    // given
    repository.saveAndFlush(article("https://example.com/duplicate"));
    // when / then
    assertThrows(DataIntegrityViolationException.class,
        () -> repository.saveAndFlush(article("https://example.com/duplicate")));
  }
}
