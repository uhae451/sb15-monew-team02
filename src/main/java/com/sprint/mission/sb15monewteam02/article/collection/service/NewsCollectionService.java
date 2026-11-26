package com.sprint.mission.sb15monewteam02.article.collection.service;

import com.sprint.mission.sb15monewteam02.article.collection.client.NaverNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.client.RssNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.mapper.NewsArticleMapper;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsCollectionService {

  private final NaverNewsClient naverNewsClient;
  private final RssNewsClient rssNewsClient;
  private final NewsArticleMapper newsArticleMapper;

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
}