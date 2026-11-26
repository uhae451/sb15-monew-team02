package com.sprint.mission.sb15monewteam02.comment.port;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String nickname
) {
}
