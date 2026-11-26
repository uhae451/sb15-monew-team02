package com.sprint.mission.sb15monewteam02;

import com.sprint.mission.sb15monewteam02.article.dto.*;
import com.sprint.mission.sb15monewteam02.article.entity.*;
import com.sprint.mission.sb15monewteam02.article.exception.*;
import com.sprint.mission.sb15monewteam02.article.repository.*;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ArticleService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArticlePersistenceTest {
  @Autowired ArticleService service;
  @Autowired ArticleRepository articles;
  @Autowired ArticleViewRepository views;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void cleanTestRows() {
    views.deleteAllInBatch();
    articles.deleteAll();
  }

  ArticleCreateRequest request(String suffix) {
    return new ArticleCreateRequest("NAVER", "https://example.com/" + suffix, "Java",
        Instant.parse("2026-11-20T10:00:00Z"), "Spring");
  }

  @Test
  void concurrentViewsBySameUserAreCountedOnce() throws Exception {
    UUID id = service.create(request("concurrent")).getId();
    UUID user = UUID.randomUUID();
    List<Boolean> results = parallel(6, () -> service.recordView(id, user));
    assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, service.getById(id).getViewCount());
    assertEquals(1, views.countByArticleId(id));
  }

  @Test
  void concurrentViewsByDifferentUsersAreAllCounted() throws Exception {
    UUID id = service.create(request("different")).getId();
    assertTrue(parallel(6, () -> service.recordView(id, UUID.randomUUID())).stream().allMatch(Boolean::booleanValue));
    assertEquals(6, service.getById(id).getViewCount());
    assertEquals(6, views.countByArticleId(id));
  }

  @Test
  void databaseRejectsDuplicateViewEvenWithoutService() {
    Article article = service.create(request("unique-view"));
    UUID user = UUID.randomUUID();
    views.saveAndFlush(new ArticleView(article, user));
    assertThrows(DataIntegrityViolationException.class, () -> views.saveAndFlush(new ArticleView(article, user)));
    assertEquals(1, views.countByArticleId(article.getId()));
  }

  @Test
  void viewInsertionRollsBackWhenCounterUpdateFails() {
    UUID id = service.create(request("rollback")).getId();
    jdbc.execute("alter table article_test.articles add constraint test_reject_view_count check (view_count = 0)");
    try {
      assertThrows(DataIntegrityViolationException.class, () -> service.recordView(id, UUID.randomUUID()));
      assertEquals(0, views.countByArticleId(id));
      assertEquals(0, service.getById(id).getViewCount());
    } finally {
      jdbc.execute("alter table article_test.articles drop constraint test_reject_view_count");
    }
  }

  @Test
  void softDeletionPersistsAndPreservesDuplicateProtection() {
    Article article = service.create(request("soft-delete"));
    service.softDelete(article.getId());
    service.softDelete(article.getId());
    assertTrue(articles.findById(article.getId()).orElseThrow().isDeleted());
    assertThrows(ArticleNotFoundException.class, () -> service.getById(article.getId()));
    assertThrows(ArticleNotFoundException.class, () -> service.recordView(article.getId(), UUID.randomUUID()));
    assertFalse(service.saveIfNew(request("soft-delete")));
  }

  @Test
  void hardDeletionRemovesViewsAndInterestLinks() {
    Article article = new Article("NAVER", "https://example.com/hard", "Java", Instant.now(), "Spring");
    article.replaceInterests(Set.of(UUID.randomUUID()));
    article = articles.saveAndFlush(article);
    service.recordView(article.getId(), UUID.randomUUID());
    service.softDelete(article.getId());
    service.hardDelete(article.getId());
    assertFalse(articles.existsById(article.getId()));
    assertEquals(0, views.countByArticleId(article.getId()));
    assertEquals(0, jdbc.queryForObject("select count(*) from article_test.article_interests where article_id = ?", Long.class, article.getId()));
  }

  @Test
  void concurrentDuplicateCreationStoresOneArticle() throws Exception {
    List<Boolean> results = parallel(4, () -> {
      try {
        service.create(request("same-url"));
        return true;
      } catch (DuplicateArticleException | DataIntegrityViolationException expected) {
        return false;
      }
    });
    assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, articles.count());
  }

  @Test
  void searchMapsViewedStatusFromDatabase() {
    Article article = service.create(request("viewed-status"));
    UUID user = UUID.randomUUID();
    service.recordView(article.getId(), user);
    var search = new ArticleSearchRequest(null, null, Set.of(), null, null, null, null, null, null, 10);
    assertTrue(service.search(search, user).content().get(0).viewedByMe());
    assertFalse(service.search(search, UUID.randomUUID()).content().get(0).viewedByMe());
    assertFalse(service.search(search, null).content().get(0).viewedByMe());
  }

  private List<Boolean> parallel(int count, Callable<Boolean> operation) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(count);
    CountDownLatch ready = new CountDownLatch(count);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        futures.add(executor.submit(() -> {
          ready.countDown();
          if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
          return operation.call();
        }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      List<Boolean> results = new ArrayList<>();
      for (Future<Boolean> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
      return results;
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }
}
