package com.sprint.mission.sb15monewteam02.interest.repository;

import org.springframework.data.domain.Sort;

import java.util.UUID;

public record InterestSearchCondition(
        String keyword,
        InterestOrderBy orderBy,
        Sort.Direction direction,
        String cursor,
        UUID after,
        int limit
) {
}
