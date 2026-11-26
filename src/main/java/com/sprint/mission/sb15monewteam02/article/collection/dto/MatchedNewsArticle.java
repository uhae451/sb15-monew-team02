package com.sprint.mission.sb15monewteam02.article.collection.dto;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleCreateRequest;
import java.util.Set;
import java.util.UUID;

// 수집한 기사 정보와 일치하는 관심사 ID 목록을 함께 관리한다.
public record MatchedNewsArticle(
    ArticleCreateRequest article,
    Set<UUID> interestIds
) {
}