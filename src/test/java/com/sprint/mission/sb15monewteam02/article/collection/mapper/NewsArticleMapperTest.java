package com.sprint.mission.sb15monewteam02.article.collection.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsItem;
import com.sprint.mission.sb15monewteam02.article.collection.dto.RssNewsItem;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NewsArticleMapperTest {

  private final NewsArticleMapper mapper = new NewsArticleMapper();

  @Test
  void 네이버_뉴스_데이터_변환_테스트() {

    // given
    NaverNewsItem item = new NaverNewsItem(
        "<b>Java</b> 뉴스",
        "https://example.com/news/1",
        "https://example.com/naver/1",
        "Java &amp; Spring",
        "Thu, 26 Nov 2026 10:00:00 +0900"
    );

    // when
    var result = mapper.fromNaver(item);

    // then
    assertEquals("NAVER", result.source());
    assertEquals("Java 뉴스", result.title());
    assertEquals("https://example.com/news/1", result.sourceUrl());
    assertEquals("Java & Spring", result.summary());
    assertEquals(
        Instant.parse("2026-11-26T01:00:00Z"),
        result.publishDate()
    );
  }

  @Test
  void RSS_뉴스_데이터_변환_테스트() {

    // given
    Instant publishDate = Instant.parse("2026-11-26T01:00:00Z");

    RssNewsItem item = new RssNewsItem(
        "한국경제",
        "Java 뉴스",
        "https://example.com/news/2",
        "Spring 관련 기사",
        publishDate
    );

    // when
    var result = mapper.fromRss(item);

    // then
    assertEquals("한국경제", result.source());
    assertEquals("Java 뉴스", result.title());
    assertEquals("https://example.com/news/2", result.sourceUrl());
    assertEquals("Spring 관련 기사", result.summary());
    assertEquals(publishDate, result.publishDate());
  }
}