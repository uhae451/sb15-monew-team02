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
import com.sprint.mission.sb15monewteam02.article.collection.dto.MatchedNewsArticle;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;
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

  private final ArticleService articleService =
      mock(ArticleService.class);

  private final NewsArticleMapper newsArticleMapper =
      spy(new NewsArticleMapper());

  private final NewsCollectionService service =
      new NewsCollectionService(
          naverNewsClient,
          rssNewsClient,
          newsArticleMapper,
          new NewsKeywordFilter(),
          articleService
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

  // 신규 기사 저장 및 저장 건수 반환을 검증한다.
  @Test
  void 신규_기사를_저장하고_저장_건수를_반환한다() {

    var article = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        Instant.now(),
        "Spring 관련 기사"
    );

    Set<UUID> interestIds = Set.of(UUID.randomUUID());

    var matched = new MatchedNewsArticle(article, interestIds);

    when(articleService.saveCollectedArticle(article, interestIds))
        .thenReturn(true);

    int result = service.saveArticles(List.of(matched));

    assertEquals(1, result);

    verify(articleService)
        .saveCollectedArticle(article, interestIds);
  }

  // 중복 기사는 신규 저장 건수에 포함하지 않는다.
  @Test
  void 중복_기사는_저장_건수에_포함하지_않는다() {

    var article = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        Instant.now(),
        "Spring 관련 기사"
    );

    Set<UUID> interestIds = Set.of(UUID.randomUUID());

    var matched = new MatchedNewsArticle(article, interestIds);

    when(articleService.saveCollectedArticle(article, interestIds))
        .thenReturn(false);

    int result = service.saveArticles(List.of(matched));

    assertEquals(0, result);

    verify(articleService)
        .saveCollectedArticle(article, interestIds);
  }

  // 저장 대상 기사가 없으면 저장 서비스를 호출하지 않는다.
  @Test
  void 저장할_기사가_없으면_저장하지_않는다() {

    int result = service.saveArticles(List.of());

    assertEquals(0, result);

    verifyNoInteractions(articleService);
  }

  // 일부 기사 저장에 실패해도 나머지 기사를 계속 저장한다.
  @Test
  void 기사_저장에_실패해도_다음_기사를_저장한다() {

    var article1 = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        Instant.now(),
        "Java 관련 기사"
    );

    var article2 = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/2",
        "Spring 뉴스",
        Instant.now(),
        "Spring 관련 기사"
    );

    Set<UUID> interestIds = Set.of(UUID.randomUUID());

    var matched1 = new MatchedNewsArticle(article1, interestIds);
    var matched2 = new MatchedNewsArticle(article2, interestIds);

    when(articleService.saveCollectedArticle(article1, interestIds))
        .thenThrow(new RuntimeException("기사 저장 실패"));

    when(articleService.saveCollectedArticle(article2, interestIds))
        .thenReturn(true);

    int result = service.saveArticles(List.of(matched1, matched2));

    assertEquals(1, result);

    verify(articleService)
        .saveCollectedArticle(article2, interestIds);
  }

  // 모든 기사 저장에 실패하면 신규 저장 건수 0을 반환한다.
  @Test
  void 모든_기사_저장에_실패하면_예외가_발생한다() {

    var article = new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        Instant.now(),
        "Java 관련 기사"
    );

    Set<UUID> interestIds = Set.of(UUID.randomUUID());

    var matched = new MatchedNewsArticle(article, interestIds);

    when(articleService.saveCollectedArticle(article, interestIds))
        .thenThrow(new RuntimeException("기사 저장 실패"));

    assertThrows(IllegalStateException.class,
        () -> service.saveArticles(List.of(matched)));
  }

  // 뉴스 수집부터 관심사 필터링 및 저장까지 전체 흐름을 검증한다.
  @Test
  void 뉴스_수집부터_저장까지_정상적으로_수행한다() {

    UUID interestId = UUID.randomUUID();

    var item = new NaverNewsItem(
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

    when(articleService.saveCollectedArticle(
        any(ArticleCreateRequest.class), eq(Set.of(interestId))))
        .thenReturn(true);

    int result = service.collectAndSave(
        Map.of(interestId, List.of("Java")),
        Map.of()
    );

    assertEquals(1, result);

    verify(articleService).saveCollectedArticle(
        any(ArticleCreateRequest.class), eq(Set.of(interestId)));
  }

  // 네이버 뉴스 수집에 실패해도 RSS 수집과 저장을 계속 진행한다.
  @Test
  void 네이버_수집에_실패해도_RSS_수집을_계속한다() {

    UUID interestId = UUID.randomUUID();

    String source = "한국경제";
    String url = "https://example.com/rss";

    when(naverNewsClient.isEnabled()).thenReturn(true);

    when(naverNewsClient.search("Java"))
        .thenThrow(new RuntimeException("네이버 API 호출 실패"));

    var rssItem = new RssNewsItem(
        source,
        "Java 뉴스",
        "https://example.com/news/2",
        "Java 관련 기사",
        Instant.now()
    );

    when(rssNewsClient.fetch(source, url))
        .thenReturn(List.of(rssItem));

    when(articleService.saveCollectedArticle(
        any(ArticleCreateRequest.class), eq(Set.of(interestId))))
        .thenReturn(true);

    int result = service.collectAndSave(
        Map.of(interestId, List.of("Java")),
        Map.of(source, url)
    );

    assertEquals(1, result);

    verify(rssNewsClient).fetch(source, url);
  }

  // 등록된 관심사가 없으면 외부 뉴스 수집을 실행하지 않는다.
  @Test
  void 등록된_관심사가_없으면_뉴스를_수집하지_않는다() {

    int result = service.collectAndSave(
        Map.of(),
        Map.of("한국경제", "https://example.com/rss")
    );

    assertEquals(0, result);

    verifyNoInteractions(
        naverNewsClient,
        rssNewsClient,
        articleService
    );
  }

  @Test
  void 네이버_기사_변환_실패시_나머지_기사_수집() {

    // given
    NaverNewsItem invalidItem = mock(NaverNewsItem.class);
    NaverNewsItem validItem = mock(NaverNewsItem.class);
    ArticleCreateRequest article = mock(ArticleCreateRequest.class);

    when(naverNewsClient.isEnabled()).thenReturn(true);

    when(naverNewsClient.search("AI")).thenReturn(
        new NaverNewsResponse(
            "", 2, 1, 2, List.of(invalidItem, validItem)
        )
    );

    doThrow(new IllegalArgumentException("변환 실패"))
        .when(newsArticleMapper).fromNaver(invalidItem);

    doReturn(article)
        .when(newsArticleMapper).fromNaver(validItem);

    // when
    var result = service.collectNaver("AI");

    // then
    assertEquals(1, result.size());
    assertEquals(article, result.get(0));
  }

  @Test
  void RSS_기사_변환_실패시_나머지_기사_수집() {

    // given
    RssNewsItem invalidItem = mock(RssNewsItem.class);
    RssNewsItem validItem = mock(RssNewsItem.class);
    ArticleCreateRequest article = mock(ArticleCreateRequest.class);

    when(rssNewsClient.fetch("한국경제", "https://example.com/rss"))
        .thenReturn(List.of(invalidItem, validItem));

    doThrow(new IllegalArgumentException("변환 실패"))
        .when(newsArticleMapper).fromRss(invalidItem);

    doReturn(article)
        .when(newsArticleMapper).fromRss(validItem);

    // when
    var result = service.collectRss(
        "한국경제", "https://example.com/rss"
    );

    // then
    assertEquals(1, result.size());
    assertEquals(article, result.get(0));
  }
}