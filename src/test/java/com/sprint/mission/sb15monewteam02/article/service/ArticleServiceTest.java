package com.sprint.mission.sb15monewteam02.article.service;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSearchRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSlice;
import com.sprint.mission.sb15monewteam02.article.exception.DuplicateArticleException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Set;
import org.mockito.Mockito;

import com.sprint.mission.sb15monewteam02.article.entity.Article;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import java.util.Optional;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleViewRepository;
import com.sprint.mission.sb15monewteam02.article.entity.ArticleView;
import com.sprint.mission.sb15monewteam02.article.exception.ArticleNotFoundException;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ArticleServiceTest {
  @Test
  void updatesInterestIdsOnLockedArticle() {
    UUID id = UUID.randomUUID();
    var interests = Set.of(UUID.randomUUID());
    Article article = validArticle();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    articleService.replaceInterests(id, interests);
    assertEquals(interests, article.getInterestIds());
  }

  @Test
  void updatesCommentCountOnLockedArticle() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    articleService.updateCommentCount(id, 3);
    assertEquals(3, article.getCommentCount());
  }
  @Test
  void refusesHardDeleteUntilCommentCleanupPolicyIsIntegrated() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    article.updateCommentCount(1);
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    assertThrows(IllegalStateException.class, () -> articleService.hardDelete(id));
    verify(articleRepository, never()).delete(any(Article.class));
    Mockito.verifyNoInteractions(articleViewRepository);
  }
  @Test
  void returnsPageCursorAndUserViewStatus() {
    var search = new ArticleSearchRequest(
        null, null, Set.of(), null, null, null, null, null, null, 1);
    UUID user = UUID.randomUUID();
    Article article = validArticle();
    UUID articleId = UUID.randomUUID();
    ReflectionTestUtils.setField(article, "id", articleId);
    when(articleRepository.search(search)).thenReturn(new ArticleSlice(List.of(article), true, 2));
    when(articleViewRepository.findViewedArticleIds(user, List.of(articleId))).thenReturn(Set.of(articleId));
    var page = articleService.search(search, user);
    assertNotNull(page);
    assertAll(
        () -> assertEquals(article.getPublishDate().toString(), page.nextCursor()),
        () -> assertEquals(articleId.toString(), page.nextAfter()),
        () -> assertEquals(1, page.size()),
        () -> assertEquals(2, page.totalElements()),
        () -> assertTrue(page.hasNext()),
        () -> assertTrue(page.content().get(0).viewedByMe()),
        () -> assertEquals(article.getTitle(), page.content().get(0).title()));
  }

  @Test
  void emptyPageHasNoNextCursor() {
    var search = new ArticleSearchRequest(
        null, null, Set.of(), null, null, null, null, null, null, 10);
    when(articleRepository.search(search)).thenReturn(new ArticleSlice(List.of(), false, 0));
    var page = articleService.search(search, null);
    assertNotNull(page);
    assertTrue(page.content().isEmpty());
    assertNull(page.nextCursor());
    assertNull(page.nextAfter());
    assertFalse(page.hasNext());
    Mockito.verifyNoInteractions(articleViewRepository);
  }

  @Test
  void storageFailureIsPropagated() {
    var failure = new DataIntegrityViolationException("storage failure");
    when(articleRepository.save(any(Article.class))).thenThrow(failure);
    assertSame(failure, assertThrows(DataIntegrityViolationException.class,
        () -> articleService.saveIfNew(request("https://example.com/failure"))));
  }
  @Mock ArticleViewRepository articleViewRepository;

  private Article validArticle() {
    var input = request("https://example.com/article");
    return new Article(input.source(), input.sourceUrl(), input.title(), input.publishDate(), input.summary());
  }

  @Test
  void missingArticleThrowsNotFound() {
    UUID id = UUID.randomUUID();
    assertThrows(ArticleNotFoundException.class, () -> articleService.getById(id));
  }

  @Test
  void returnsActiveArticle() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    when(articleRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(article));
    assertSame(article, articleService.getById(id));
  }

  @Test
  void deletedArticleIsExcludedFromNormalLookup() {
    UUID id = UUID.randomUUID();
    when(articleRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());
    assertThrows(ArticleNotFoundException.class, () -> articleService.getById(id));
    verify(articleRepository, never()).findById(id);
  }

  @Test
  void softDeletesWithoutRemovingRow() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    articleService.softDelete(id);
    assertTrue(article.isDeleted());
    verify(articleRepository, never()).delete(any(Article.class));
  }

  @Test
  void hardDeletesViewsBeforeArticle() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    articleService.hardDelete(id);
    var order = Mockito.inOrder(articleViewRepository, articleRepository);
    order.verify(articleViewRepository).deleteAllByArticleId(id);
    order.verify(articleRepository).delete(article);
  }

  @Test
  void sameUserDoesNotIncreaseViewCountAgain() {
    UUID id = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    Article article = validArticle();
    article.recordView();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    when(articleViewRepository.existsByArticleIdAndUserId(id, user)).thenReturn(true);
    assertFalse(articleService.recordView(id, user));
    verify(articleViewRepository).existsByArticleIdAndUserId(id, user);
    assertEquals(1, article.getViewCount());
    verify(articleViewRepository, never()).save(any(ArticleView.class));
  }

  @Test
  void differentUsersEachIncreaseViewCount() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    assertTrue(articleService.recordView(id, UUID.randomUUID()));
    assertTrue(articleService.recordView(id, UUID.randomUUID()));
    assertEquals(2, article.getViewCount());
    verify(articleViewRepository, Mockito.times(2)).save(any(ArticleView.class));
  }

  @Test
  void deletedArticleCannotReceiveViews() {
    UUID id = UUID.randomUUID();
    Article article = validArticle();
    article.softDelete();
    when(articleRepository.findForUpdate(id)).thenReturn(Optional.of(article));
    assertThrows(ArticleNotFoundException.class, () -> articleService.recordView(id, UUID.randomUUID()));
    verify(articleViewRepository, never()).save(any(ArticleView.class));
  }

  @Test
  void anonymousViewIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> articleService.recordView(UUID.randomUUID(), null));
    Mockito.verifyNoInteractions(articleViewRepository);
  }
  private ArticleCreateRequest request(String url) {
    return new ArticleCreateRequest(
        "NAVER", url, "Java news", java.time.Instant.parse("2026-11-20T10:00:00Z"), "Spring news");
  }

  @Test
  void storesSubmittedInformationAndReturnsSavedArticle() {
    // given
    var input = request("https://news.example.com/3");
    Article persisted = new Article(input.source(), input.sourceUrl(), input.title(), input.publishDate(), input.summary());
    when(articleRepository.save(any(Article.class))).thenReturn(persisted);
    // when
    Article result = articleService.create(input);
    // then
    var captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository).save(captor.capture());
    assertAll(
        () -> assertSame(persisted, result),
        () -> assertEquals(input.sourceUrl(), captor.getValue().getSourceUrl()),
        () -> assertEquals(input.title(), captor.getValue().getTitle()));
  }

  @Test
  void rejectsInvalidInformationWithoutSaving() {
    // given / when / then
    assertThrows(IllegalArgumentException.class,
        () -> articleService.create(request("invalid")));
    verify(articleRepository, never()).save(any(Article.class));
  }

  @Test
  void reportsDuplicateOnCreateWithoutSaving() {
    var input = request("https://news.example.com/4");
    when(articleRepository.existsBySourceUrl(input.sourceUrl())).thenReturn(true);
    assertThrows(
        DuplicateArticleException.class,
        () -> articleService.create(input));
    verify(articleRepository, never()).save(any(Article.class));
  }

    @Mock
    ArticleRepository articleRepository;

    @InjectMocks
    ArticleService articleService;

    @Test
    void 중복된_기사는_저장하지_않는다() {

      // given: 이미 저장된 기사의 URL
      String url = "https://news.example.com/1";

      when(articleRepository.existsBySourceUrl(url))
          .thenReturn(true);

      // when: 같은 URL의 기사 저장 시도
      boolean saved = articleService.saveIfNew(request(url));

      // then: 저장되지 않아야 한다
      assertFalse(saved);

      verify(articleRepository, never())
          .save(any(Article.class));
    }

  @Test
  void 새로운_기사는_정상적으로_저장된다() {

    // given: DB에 존재하지 않는 새로운 URL
    String url = "https://news.example.com/2";

    when(articleRepository.existsBySourceUrl(url))
        .thenReturn(false);

    // when: 새로운 기사 저장 시도
    boolean saved = articleService.saveIfNew(request(url));

    // then: 저장에 성공해야 한다
    assertTrue(saved);

    // 실제 저장 메서드가 호출되었는지 검증
    verify(articleRepository).save(any(Article.class));
  }
}
