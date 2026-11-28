package com.sprint.mission.sb15monewteam02.article.collection.service;

import com.sprint.mission.sb15monewteam02.article.collection.client.NaverNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.client.RssNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.dto.MatchedNewsArticle;
import com.sprint.mission.sb15monewteam02.article.collection.filter.NewsKeywordFilter;
import com.sprint.mission.sb15monewteam02.article.collection.mapper.NewsArticleMapper;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import com.sprint.mission.sb15monewteam02.article.exception.DuplicateArticleException;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectionService {

  private final NaverNewsClient naverNewsClient;
  private final RssNewsClient rssNewsClient;
  private final NewsArticleMapper newsArticleMapper;
  private final NewsKeywordFilter newsKeywordFilter;
  private final ArticleService articleService;

  // 네이버 뉴스 수집 및 기사별 변환
  public List<ArticleCreateRequest> collectNaver(String keyword) {

    if (!naverNewsClient.isEnabled()) {
      return List.of();
    }

    List<ArticleCreateRequest> articles = new ArrayList<>();

    for (var item : naverNewsClient.search(keyword).items()) {
      try {
        articles.add(newsArticleMapper.fromNaver(item));
      } catch (RuntimeException e) {
        log.warn("네이버 뉴스 기사 변환 실패: {}", item.link(), e);
      }
    }

    return articles;
  }

  // RSS 뉴스 수집 및 기사별 변환
  public List<ArticleCreateRequest> collectRss(
      String source, String rssUrl
  ) {
    List<ArticleCreateRequest> articles = new ArrayList<>();

    for (var item : rssNewsClient.fetch(source, rssUrl)) {
      try {
        articles.add(newsArticleMapper.fromRss(item));
      } catch (RuntimeException e) {
        log.warn("RSS 뉴스 기사 변환 실패: {}", item.link(), e);
      }
    }

    return articles;
  }

  // 관심사 키워드와 일치하는 기사만 선별하고 관심사 ID를 연결한다.
  public List<MatchedNewsArticle> filterArticles(
      List<ArticleCreateRequest> articles,
      Map<UUID, List<String>> interestKeywords) {

    return articles.stream()
        .map(article -> new MatchedNewsArticle(
            article,
            newsKeywordFilter.findMatchingInterestIds(
                article, interestKeywords)
        ))
        .filter(matched -> !matched.interestIds().isEmpty())
        .toList();
  }

  // 기사 저장 및 전체 저장 실패 처리
  public int saveArticles(List<MatchedNewsArticle> articles) {

    int savedCount = 0;
    int failedCount = 0;

    for (MatchedNewsArticle matched : articles) {
      try {boolean saved = saveWithRetry(matched);

        if (saved) {
          savedCount++;
        }

      } catch (RuntimeException e) {
        failedCount++;

        log.error("뉴스 기사 저장 실패: {}",
            matched.article().sourceUrl(), e);
      }
    }

    // 모든 기사 저장에 실패한 경우 예외 발생
    if (!articles.isEmpty() && failedCount == articles.size()) {
      throw new IllegalStateException("모든 뉴스 기사 저장 실패");
    }

    return savedCount;
  }

  // 뉴스 수집, 관심사 필터링 및 저장을 수행한다.
  public int collectAndSave(
      Map<UUID, List<String>> interestKeywords,
      Map<String, String> rssSources) {

    if (interestKeywords.isEmpty()) {
      return 0;
    }

    List<ArticleCreateRequest> articles = new ArrayList<>();

    int attemptedCount = 0;
    int failedCount = 0;

    // 중복 키워드 제거
    List<String> keywords = interestKeywords.values().stream()
        .flatMap(List::stream)
        .filter(keyword -> keyword != null && !keyword.isBlank())
        .map(String::trim)
        .distinct()
        .toList();

    // 네이버 뉴스 수집
    if (naverNewsClient.isEnabled()) {
      for (String keyword : keywords) {
        attemptedCount++;

        try {
          articles.addAll(collectNaver(keyword));
        } catch (RuntimeException e) {
          failedCount++;
          log.error("네이버 뉴스 수집 실패: {}", keyword, e);
        }
      }
    }

    // RSS 뉴스 수집
    for (var entry : rssSources.entrySet()) {
      attemptedCount++;

      try {
        articles.addAll(
            collectRss(entry.getKey(), entry.getValue())
        );
      } catch (RuntimeException e) {
        failedCount++;
        log.error("RSS 뉴스 수집 실패: {}", entry.getKey(), e);
      }
    }

    // 시도한 모든 뉴스 수집이 실패한 경우
    if (attemptedCount > 0 && attemptedCount == failedCount) {
      throw new IllegalStateException("모든 뉴스 수집 실패");
    }

    return saveArticles(
        filterArticles(articles, interestKeywords)
    );
  }

  // 중복 저장 충돌이 발생하면 한 번 재시도한다.
  private boolean saveWithRetry(MatchedNewsArticle matched) {
    try {
      return articleService.saveCollectedArticle(
          matched.article(), matched.interestIds()
      );
    } catch (RuntimeException e) {
      if (!isDuplicateArticle(e)) {
        throw e;
      }

      log.warn("기사 중복 저장 충돌 발생, 재시도");

      return articleService.saveCollectedArticle(
          matched.article(), matched.interestIds()
      );
    }
  }

  // 기사 원본 URL 중복 오류인지 확인한다.
  private boolean isDuplicateArticle(Throwable error) {
    for (Throwable cause = error;
        cause != null;
        cause = cause.getCause()) {

      if (cause instanceof DuplicateArticleException) {
        return true;
      }

      if (cause instanceof ConstraintViolationException violation
          && "uk_articles_source_url".equalsIgnoreCase(
          violation.getConstraintName())) {
        return true;
      }
    }

    return false;
  }
}