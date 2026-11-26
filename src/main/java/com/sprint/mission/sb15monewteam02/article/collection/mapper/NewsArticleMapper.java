package com.sprint.mission.sb15monewteam02.article.collection.mapper;

import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsItem;
import com.sprint.mission.sb15monewteam02.article.collection.dto.RssNewsItem;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class NewsArticleMapper {

  public ArticleCreateRequest fromNaver(NaverNewsItem item) {

    return new ArticleCreateRequest(
        "NAVER",
        item.originallink() == null || item.originallink().isBlank()
            ? item.link() : item.originallink(),
        cleanText(item.title()),
        ZonedDateTime.parse(
            item.pubDate(),
            DateTimeFormatter.RFC_1123_DATE_TIME
        ).toInstant(),
        cleanText(item.description())
    );
  }

  public ArticleCreateRequest fromRss(RssNewsItem item) {

    return new ArticleCreateRequest(
        item.source(),
        item.link(),
        cleanText(item.title()),
        item.pubDate(),
        cleanText(item.description())
    );
  }

  private String cleanText(String text) {
    if (text == null) {
      return null;
    }

    return HtmlUtils.htmlUnescape(
        text.replaceAll("(?i)</?b>", "")
    ).trim();
  }
}