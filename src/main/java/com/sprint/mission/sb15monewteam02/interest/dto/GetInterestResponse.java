package com.sprint.mission.sb15monewteam02.interest.dto;

import java.util.List;

public record GetInterestResponse(
        List<InterestDto> content,
        String nextCursor,
        String nextAfter,
        int size,
        long totalElements,
        boolean hasNext
) {
}
