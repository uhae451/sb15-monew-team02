package com.sprint.mission.sb15monewteam02.article.collection.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NewsKeywordFilterTest {

  private final NewsKeywordFilter filter = new NewsKeywordFilter();

  // 테스트에 사용할 기사 데이터를 생성한다.
  private ArticleCreateRequest article(String title, String summary) {
    return new ArticleCreateRequest(
        "NAVER",
        "https://example.com/news/1",
        title,
        Instant.now(),
        summary
    );
  }

  @Test
  void 제목에_키워드가_포함되면_통과한다() {
    var article = article("Java 신규 버전 출시", "기술 소식");

    assertTrue(filter.matches(article, List.of("Java")));
  }

  @Test
  void 요약에_키워드가_포함되면_통과한다() {
    var article = article("최신 기술 소식", "Spring Boot 업데이트");

    assertTrue(filter.matches(article, List.of("Spring")));
  }

  @Test
  void 키워드가_포함되지_않으면_제외한다() {
    var article = article("부동산 시장 동향", "아파트 가격 상승");

    assertFalse(filter.matches(article, List.of("Java")));
  }

  @Test
  void 대소문자와_앞뒤_공백을_구분하지_않는다() {
    var article = article("JAVA 뉴스", "기술 소식");

    assertTrue(filter.matches(article, List.of(" java ")));
  }

  @Test
  void 여러_키워드_중_하나만_일치해도_통과한다() {
    var article = article("Java 뉴스", "기술 소식");

    assertTrue(filter.matches(
        article, List.of("경제", "스포츠", "Java")));
  }

  @Test
  void 키워드_목록이_비어있으면_제외한다() {
    var article = article("Java 뉴스", "기술 소식");

    assertFalse(filter.matches(article, List.of()));
  }

  @Test
  void 공백_키워드는_무시한다() {
    var article = article("Java 뉴스", "기술 소식");

    assertFalse(filter.matches(article, List.of("", " ")));
  }

  @Test
  void 일치하는_모든_관심사_ID를_반환한다() {

    // given
    UUID backendId = UUID.randomUUID();
    UUID economyId = UUID.randomUUID();
    UUID programmingId = UUID.randomUUID();

    var article = article("Java와 Spring 뉴스", "백엔드 기술");

    Map<UUID, List<String>> interests = Map.of(
        backendId, List.of("Spring"),
        economyId, List.of("주식", "금리"),
        programmingId, List.of("Java")
    );

    // when
    var result = filter.findMatchingInterestIds(article, interests);

    // then
    assertEquals(Set.of(backendId, programmingId), result);
  }

  @Test
  void 일치하는_관심사가_없으면_빈_Set을_반환한다() {

    // given
    var article = article("Java 뉴스", "기술 소식");

    Map<UUID, List<String>> interests = Map.of(
        UUID.randomUUID(), List.of("주식", "금리")
    );

    // when
    var result = filter.findMatchingInterestIds(article, interests);

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void 등록된_관심사가_없으면_빈_Set을_반환한다() {

    // given
    var article = article("Java 뉴스", "기술 소식");

    // when
    var result = filter.findMatchingInterestIds(article, Map.of());

    // then
    assertTrue(result.isEmpty());
  }
}