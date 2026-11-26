package com.sprint.mission.sb15monewteam02.article.collection.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sprint.mission.sb15monewteam02.article.collection.dto.NaverNewsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(NaverNewsClient.class)
@TestPropertySource(locations = "file:./.env")
@EnabledIfEnvironmentVariable(
    named = "RUN_NAVER_API_TEST",
    matches = "true"
)
class NaverNewsClientTest {

  @Autowired
  private NaverNewsClient naverNewsClient;

  @Test
  void 네이버_뉴스_API_호출_테스트() {

    // given
    String keyword = "인공지능";
    assertTrue(naverNewsClient.isEnabled());

    // when
    NaverNewsResponse response = naverNewsClient.search(keyword);

    // then
    assertNotNull(response);
    assertNotNull(response.items());
    assertFalse(response.items().isEmpty());

    System.out.println("수집된 기사 수: " + response.items().size());
  }
}