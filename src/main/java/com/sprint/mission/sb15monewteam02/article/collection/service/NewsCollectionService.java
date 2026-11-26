package com.sprint.mission.sb15monewteam02.article.collection.service;

import com.sprint.mission.sb15monewteam02.article.collection.client.NaverNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.client.RssNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.dto.MatchedNewsArticle;
import com.sprint.mission.sb15monewteam02.article.collection.filter.NewsKeywordFilter;
import com.sprint.mission.sb15monewteam02.article.collection.mapper.NewsArticleMapper;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsCollectionService {

  private final NaverNewsClient naverNewsClient;
  private final RssNewsClient rssNewsClient;
  private final NewsArticleMapper newsArticleMapper;
  private final NewsKeywordFilter newsKeywordFilter;

  // 네이버 API를 통해 키워드에 해당하는 뉴스를 수집하고 기사 등록 DTO로 변환한다.
  public List<ArticleCreateRequest> collectNaver(String keyword) {

    // API 인증키가 없는 팀원도 다른 기능을 정상적으로 사용할 수 있도록 네이버 뉴스 수집만 건너뛴다.
    if (!naverNewsClient.isEnabled()) {
      return List.of();
    }

    return naverNewsClient.search(keyword)
        .items()
        .stream()
        .map(newsArticleMapper::fromNaver)
        .toList();
  }

  // 언론사의 RSS 주소를 통해 뉴스를 수집하고 기사 등록 DTO로 변환한다.
  public List<ArticleCreateRequest> collectRss(
      String source, String rssUrl) {

    return rssNewsClient.fetch(source, rssUrl)
        .stream()
        .map(newsArticleMapper::fromRss)
        .toList();
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
}