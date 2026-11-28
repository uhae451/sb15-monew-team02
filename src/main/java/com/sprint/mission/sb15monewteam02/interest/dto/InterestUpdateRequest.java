package com.sprint.mission.sb15monewteam02.interest.dto;

import java.util.List;

// 관심사 키워드 수정 요청 (프론트: PATCH /api/interests/{id}, body {keywords})
public record InterestUpdateRequest(
        List<String> keywords
) {
}
