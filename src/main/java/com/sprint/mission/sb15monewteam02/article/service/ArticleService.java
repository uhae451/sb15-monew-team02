package com.sprint.mission.sb15monewteam02.article.service;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticlePage;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleResponse;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSearchRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleViewResponse;
import com.sprint.mission.sb15monewteam02.article.entity.Article;
import com.sprint.mission.sb15monewteam02.article.entity.ArticleView;
import com.sprint.mission.sb15monewteam02.article.exception.ArticleNotFoundException;
import com.sprint.mission.sb15monewteam02.article.exception.DuplicateArticleException;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleRepository;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleViewRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;

  public ArticleService(ArticleRepository articleRepository, ArticleViewRepository articleViewRepository) {
    this.articleRepository = articleRepository;
    this.articleViewRepository = articleViewRepository;
  }

  public Article create(ArticleCreateRequest request) {
    if (request == null) throw new IllegalArgumentException("request is required");
    Article article = new Article(request.source(), request.sourceUrl(), request.title(),
        request.publishDate(), request.summary());
    if (articleRepository.existsBySourceUrl(request.sourceUrl())) {
      throw new DuplicateArticleException();
    }
    // Concurrent inserts are also protected by the database UNIQUE constraint.
    // A storage failure is propagated so that the transaction rolls back.
    return articleRepository.save(article);
  }

  public boolean saveIfNew(ArticleCreateRequest request) {
    try {
      create(request);
      return true;
    } catch (DuplicateArticleException duplicate) {
      return false;
    }
  }

  @Transactional(readOnly = true)
  public List<String> getSources() {
    return List.of("NAVER", "한국경제", "조선일보", "연합뉴스");
  }

  @Transactional(readOnly = true)
  public ArticleResponse getArticleResponse(UUID articleId, UUID userId) {

    Article article = getById(articleId);

    boolean viewedByMe = userId != null &&
        articleViewRepository.existsByArticleIdAndUserId(articleId, userId);

    return new ArticleResponse(
        article.getId(),
        article.getSource(),
        article.getSourceUrl(),
        article.getTitle(),
        article.getPublishDate(),
        article.getSummary(),
        article.getCommentCount(),
        article.getViewCount(),
        viewedByMe
    );
  }

  @Transactional(readOnly = true)
  public Article getById(UUID id) {
    return articleRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ArticleNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public ArticlePage search(ArticleSearchRequest request, UUID requestUserId) {
    if (request == null) throw new IllegalArgumentException("request is required");
    var slice = articleRepository.search(request);
    var ids = slice.content().stream().map(Article::getId).toList();
    Set<UUID> viewed = requestUserId == null || ids.isEmpty()
        ? Set.of() : articleViewRepository.findViewedArticleIds(requestUserId, ids);
    var content = slice.content().stream().map(a -> new ArticleResponse(
        a.getId(), a.getSource(), a.getSourceUrl(), a.getTitle(), a.getPublishDate(), a.getSummary(),
        a.getCommentCount(), a.getViewCount(), viewed.contains(a.getId()))).toList();
    String cursor = null;
    String after = null;
    if (slice.hasNext() && !content.isEmpty()) {
      Article last = slice.content().get(content.size() - 1);
      cursor = switch (request.orderBy()) {
        case publishDate -> last.getPublishDate().toString();
        case viewCount -> Long.toString(last.getViewCount());
        case commentCount -> Long.toString(last.getCommentCount());
      };
      after = last.getId().toString();
    }
    return new ArticlePage(content, cursor, after, content.size(), slice.totalElements(), slice.hasNext());
  }

  public void softDelete(UUID id) {
    lockArticle(id).softDelete();
  }

  public void hardDelete(UUID id) {
    Article article = lockArticle(id);
    // Replace this guard with agreed comment cleanup when the comment domain is available.
    if (article.getCommentCount() != 0) {
      throw new IllegalStateException("Comment cleanup must be integrated before deleting an article with comments");
    }
    articleViewRepository.deleteAllByArticleId(id);
    articleRepository.delete(article);
  }

  public boolean recordView(UUID id, UUID userId) {
    if (userId == null) throw new IllegalArgumentException("userId is required");
    // Serialize view creation and counter changes for this article in one transaction.
    Article article = lockActiveArticle(id);
    if (articleViewRepository.existsByArticleIdAndUserId(id, userId)) return false;
    articleViewRepository.save(new ArticleView(article, userId));
    article.recordView();
    return true;
  }

  public ArticleViewResponse recordViewResponse(UUID articleId, UUID userId) {

    recordView(articleId, userId);

    ArticleView view = articleViewRepository
        .findByArticleIdAndUserId(articleId, userId)
        .orElseThrow();

    Article article = view.getArticle();

    return new ArticleViewResponse(
        view.getId(),
        view.getUserId(),
        view.getCreatedAt(),
        article.getId(),
        article.getSource(),
        article.getSourceUrl(),
        article.getTitle(),
        article.getPublishDate(),
        article.getSummary(),
        article.getCommentCount(),
        article.getViewCount()
    );
  }

  /** ID-only integration point; the interest domain must validate that IDs exist. */
  public void replaceInterests(UUID id, Set<UUID> interestIds) {
    lockActiveArticle(id).replaceInterests(interestIds);
  }

  /** The comment domain must supply its authoritative count in the same transaction. */
  public void updateCommentCount(UUID id, long count) {
    lockArticle(id).updateCommentCount(count);
  }

  private Article lockArticle(UUID id) {
    return articleRepository.findForUpdate(id).orElseThrow(() -> new ArticleNotFoundException(id));
  }

  private Article lockActiveArticle(UUID id) {
    Article article = lockArticle(id);
    if (article.isDeleted()) throw new ArticleNotFoundException(id);
    return article;
  }

  // 수집한 기사를 저장하고 기존 기사에는 새로운 관심사만 추가한다.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean saveCollectedArticle(
      ArticleCreateRequest request,
      Set<UUID> interestIds) {

    // 일치하는 관심사가 없는 기사는 저장하지 않는다.
    if (interestIds.isEmpty()) {
      return false;
    }

    var existing = articleRepository.findBySourceUrl(request.sourceUrl());

    // 이미 저장된 기사를 처리한다.
    if (existing.isPresent()) {

      Article article = existing.get();

      // 논리 삭제된 기사는 다시 등록하지 않는다.
      if (article.isDeleted()) {
        return false;
      }

      // 기존 관심사를 유지하면서 새로운 관심사를 추가한다.
      Set<UUID> merged = new HashSet<>(article.getInterestIds());
      merged.addAll(interestIds);

      article.replaceInterests(merged);

      return false;
    }

    // 신규 기사를 저장하고 관심사를 연결한다.
    Article article = create(request);
    article.replaceInterests(interestIds);

    return true;
  }
}
