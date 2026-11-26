package com.sprint.mission.sb15monewteam02.article.collection.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.mission.sb15monewteam02.article.collection.client.NaverNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.client.RssNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsItem;
import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsResponse;
import com.sprint.mission.sb15monewteam02.article.collection.dto.RssNewsItem;
import com.sprint.mission.sb15monewteam02.article.collection.mapper.NewsArticleMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewsCollectionServiceTest {

  private final NaverNewsClient naverNewsClient =
      mock(NaverNewsClient.class);

  private final RssNewsClient rssNewsClient =
      mock(RssNewsClient.class);

  private final NewsCollectionService service =
      new NewsCollectionService(
          naverNewsClient,
          rssNewsClient,
          new NewsArticleMapper()
      );

  @Test
  void 네이버_뉴스_수집_테스트() {

    // given
    NaverNewsItem item = new NaverNewsItem(
        "Java 뉴스",
        "https://example.com/news/1",
        "https://example.com/naver/1",
        "Spring 관련 기사",
        "Thu, 26 Nov 2026 10:00:00 +0900"
    );

    when(naverNewsClient.isEnabled()).thenReturn(true);

    when(naverNewsClient.search("Java"))
        .thenReturn(new NaverNewsResponse(
            "", 1, 1, 1, List.of(item)
        ));

    // when
    var result = service.collectNaver("Java");

    // then
    assertEquals(1, result.size());
    assertEquals("NAVER", result.get(0).source());
    assertEquals("Java 뉴스", result.get(0).title());
  }

  @Test
  void API_키가_없으면_네이버_수집을_건너뛴다() {

    // given
    when(naverNewsClient.isEnabled()).thenReturn(false);

    // when
    var result = service.collectNaver("Java");

    // then
    assertTrue(result.isEmpty());
    verify(naverNewsClient, never()).search(anyString());
  }

  @Test
  void RSS_뉴스_수집_테스트() {

    // given
    String source = "한국경제";
    String url = "https://example.com/rss";

    RssNewsItem item = new RssNewsItem(
        source,
        "Java 뉴스",
        "https://example.com/news/2",
        "Spring 관련 기사",
        Instant.now()
    );

    when(rssNewsClient.fetch(source, url))
        .thenReturn(List.of(item));

    // when
    var result = service.collectRss(source, url);

    // then
    assertEquals(1, result.size());
    assertEquals(source, result.get(0).source());
    assertEquals("Java 뉴스", result.get(0).title());
  }
}