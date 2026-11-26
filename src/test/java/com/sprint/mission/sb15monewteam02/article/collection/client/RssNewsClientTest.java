package com.sprint.mission.sb15monewteam02.article.collection.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sprint.mission.sb15monewteam02.article.collection.dto.RssNewsItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class RssNewsClientTest {

  private final RssNewsClient rssNewsClient = new RssNewsClient();

  @Test
  void 한국경제_RSS_호출_테스트() {

    // given
    String source = "한국경제";
    String rssUrl = "https://www.hankyung.com/feed/all-news";

    // when
    List<RssNewsItem> items = rssNewsClient.fetch(source, rssUrl);

    // then
    assertNotNull(items);
    assertFalse(items.isEmpty());
    assertEquals(source, items.get(0).source());

    System.out.println("수집된 기사 수: " + items.size());
    System.out.println("기사 제목: " + items.get(0).title());
  }

  @Test
  void 조선일보_RSS_호출_테스트() {

    // given
    String source = "조선일보";
    String rssUrl = "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml";

    // when
    List<RssNewsItem> items = rssNewsClient.fetch(source, rssUrl);

    // then
    assertNotNull(items);
    assertFalse(items.isEmpty());
    assertEquals(source, items.get(0).source());

    System.out.println("수집된 기사 수: " + items.size());
    System.out.println("기사 제목: " + items.get(0).title());
  }

  @Test
  void 연합뉴스TV_RSS_호출_테스트() {

    // given
    String source = "연합뉴스";
    String rssUrl = "https://www.yonhapnewstv.co.kr/browse/feed/";

    // when
    List<RssNewsItem> items = rssNewsClient.fetch(source, rssUrl);

    // then
    assertNotNull(items);
    assertFalse(items.isEmpty());
    assertEquals(source, items.get(0).source());

    System.out.println("수집된 기사 수: " + items.size());
    System.out.println("기사 제목: " + items.get(0).title());
  }

}