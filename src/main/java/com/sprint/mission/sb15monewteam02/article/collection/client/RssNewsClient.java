package com.sprint.mission.sb15monewteam02.article.collection.client;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.sprint.mission.sb15monewteam02.article.collection.dto.RssNewsItem;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RssNewsClient {

  private final RestClient restClient = RestClient.create();

  public List<RssNewsItem> fetch(String source, String rssUrl) {

    try {
      byte[] xml = restClient.get()
          .uri(rssUrl)
          .retrieve()
          .body(byte[].class);

      try (XmlReader reader = new XmlReader(new ByteArrayInputStream(xml))) {

        SyndFeed feed = new SyndFeedInput().build(reader);

        return feed.getEntries().stream()
            .map(entry -> new RssNewsItem(
                source,
                entry.getTitle(),
                entry.getLink(),
                entry.getDescription() == null
                    ? null : entry.getDescription().getValue(),
                entry.getPublishedDate() == null
                    ? null : entry.getPublishedDate().toInstant()
            ))
            .toList();
      }

    } catch (Exception e) {
      throw new IllegalStateException("RSS 뉴스 수집 실패: " + source, e);
    }
  }
}