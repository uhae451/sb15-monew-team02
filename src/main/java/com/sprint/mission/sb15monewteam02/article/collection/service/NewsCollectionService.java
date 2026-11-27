package com.sprint.mission.sb15monewteam02.article.collection.service;

import com.sprint.mission.sb15monewteam02.article.collection.client.NaverNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.client.RssNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.dto.MatchedNewsArticle;
import com.sprint.mission.sb15monewteam02.article.collection.filter.NewsKeywordFilter;
import com.sprint.mission.sb15monewteam02.article.collection.mapper.NewsArticleMapper;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
      try {
        boolean saved = articleService.saveCollectedArticle(
            matched.article(),
            matched.interestIds()
        );

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

  // 뉴스 수집, 관심사 필터링, 기사 저장을 순차적으로 수행한다.
  public int collectAndSave(
      Map<UUID, List<String>> interestKeywords,
      Map<String, String> rssSources) {

    // 등록된 관심사가 없으면 뉴스 수집을 진행하지 않는다.
    if (interestKeywords.isEmpty()) {
      return 0;
    }

    List<ArticleCreateRequest> articles = new ArrayList<>();

    // 중복 키워드를 제거하고 네이버 뉴스를 수집한다.
    interestKeywords.values().stream()
        .flatMap(List::stream)
        .filter(keyword -> keyword != null && !keyword.isBlank())
        .map(String::trim)
        .distinct()
        .forEach(keyword -> {
          try {
            articles.addAll(collectNaver(keyword));
          } catch (RuntimeException e) {
            log.error("네이버 뉴스 수집 실패: {}", keyword, e);
          }
        });

    // 언론사별 RSS 뉴스를 수집한다.
    rssSources.forEach((source, url) -> {
      try {
        articles.addAll(collectRss(source, url));
      } catch (RuntimeException e) {
        log.error("RSS 뉴스 수집 실패: {}", source, e);
      }
    });

    // 관심사와 일치하는 기사를 선별하고 저장한다.
    return saveArticles(filterArticles(articles, interestKeywords));
  }
}