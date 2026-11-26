package com.sprint.mission.sb15monewteam02.article.collection.client;

import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverNewsClient {

  private final RestClient restClient = RestClient.create();

  @Value("${NAVER_CLIENT_ID:}")
  private String clientId;

  @Value("${NAVER_CLIENT_SECRET:}")
  private String clientSecret;

  @Value("${NAVER_NEWS_URL:}")
  private String newsUrl;

  public boolean isEnabled() {
    return !clientId.isBlank()
        && !clientSecret.isBlank()
        && !newsUrl.isBlank();
  }

  public NaverNewsResponse search(String keyword) {

    if (!isEnabled()) {
      throw new IllegalStateException("네이버 뉴스 API가 비활성화되어 있습니다.");
    }

    return restClient.get()
        .uri(newsUrl + "?query={keyword}&display=10&sort=date", keyword)
        .header("X-NCP-APIGW-API-KEY-ID", clientId)
        .header("X-NCP-APIGW-API-KEY", clientSecret)
        .retrieve()
        .body(NaverNewsResponse.class);
  }
}