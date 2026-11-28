package com.sprint.mission.sb15monewteam02.interest.dto;

import java.util.List;

public record InterestRegisterRequest(
        String name,
        List<String> keywords
) {
}
