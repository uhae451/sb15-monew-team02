package com.sprint.mission.sb15monewteam02.article.collection.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.mission.sb15monewteam02.article.collection.client.NaverNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.client.RssNewsClient;
import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsItem;
import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsResponse;
import com.sprint.mission.sb15monewteam02.article.collection.dto.RssNewsItem;
import com.sprint.mission.sb15monewteam02.article.collection.filter.NewsKeywordFilter;
import com.sprint.mission.sb15monewteam02.article.collection.mapper.NewsArticleMapper;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
          new NewsArticleMapper(),
          new NewsKeywordFilter()
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

  // 관심사 키워드와 일치하는 기사만 선별하는지 검증한다.
  @Test
  void 관심사와_일치하는_기사만_반환한다() {

    UUID interestId = UUID.randomUUID();

    var javaArticle = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        Instant.now(),
        "백엔드 기술 소식"
    );

    var economyArticle = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/2",
        "경제 뉴스",
        Instant.now(),
        "금리 상승"
    );

    var result = service.filterArticles(
        List.of(javaArticle, economyArticle),
        Map.of(interestId, List.of("Java"))
    );

    assertEquals(1, result.size());
    assertEquals(javaArticle, result.get(0).article());
    assertEquals(Set.of(interestId), result.get(0).interestIds());
  }

  // 하나의 기사에 여러 관심사 ID가 연결되는지 검증한다.
  @Test
  void 하나의_기사에_여러_관심사를_연결한다() {

    UUID backendId = UUID.randomUUID();
    UUID programmingId = UUID.randomUUID();

    var article = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java와 Spring 뉴스",
        Instant.now(),
        "개발 기술 소식"
    );

    var result = service.filterArticles(
        List.of(article),
        Map.of(
            backendId, List.of("Spring"),
            programmingId, List.of("Java")
        )
    );

    assertEquals(1, result.size());
    assertEquals(
        Set.of(backendId, programmingId),
        result.get(0).interestIds()
    );
  }

  // 등록된 관심사가 없으면 기사를 선별하지 않는다.
  @Test
  void 등록된_관심사가_없으면_빈_리스트를_반환한다() {

    var article = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        Instant.now(),
        "기술 소식"
    );

    var result = service.filterArticles(
        List.of(article),
        Map.of()
    );

    assertTrue(result.isEmpty());
  }
}