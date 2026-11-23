package com.sprint.mission.sb15monewteam02.article.service;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticlePage;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleResponse;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSearchRequest;
import com.sprint.mission.sb15monewteam02.article.entity.Article;
import com.sprint.mission.sb15monewteam02.article.entity.ArticleView;
import com.sprint.mission.sb15monewteam02.article.exception.ArticleNotFoundException;
import com.sprint.mission.sb15monewteam02.article.exception.DuplicateArticleException;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleRepository;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleViewRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
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
}
