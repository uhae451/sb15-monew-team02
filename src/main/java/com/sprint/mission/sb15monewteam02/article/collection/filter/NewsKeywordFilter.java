package com.sprint.mission.sb15monewteam02.article.collection.filter;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;


@Component
public class NewsKeywordFilter {

  // 기사 제목 또는 요약에 관심사 키워드가 포함되어 있는지 확인한다.
  public boolean matches(
      ArticleCreateRequest article, List<String> keywords) {

    String title = normalize(article.title());
    String summary = normalize(article.summary());

    return keywords.stream()
        .filter(keyword -> keyword != null && !keyword.isBlank())
        .map(this::normalize)
        .anyMatch(keyword ->
            title.contains(keyword) || summary.contains(keyword));
  }

  // 대소문자 차이와 앞뒤 공백을 제거한다.
  private String normalize(String text) {
    return text == null
        ? ""
        : text.trim().toLowerCase(Locale.ROOT);
  }

  // 기사와 일치하는 모든 관심사의 ID를 반환한다.
  public Set<UUID> findMatchingInterestIds(
      ArticleCreateRequest article,
      Map<UUID, List<String>> interestKeywords) {

    return interestKeywords.entrySet().stream()
        .filter(entry -> matches(article, entry.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }


}