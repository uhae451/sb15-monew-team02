package com.sprint.mission.sb15monewteam02.comment.event;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID articleId,
        UUID userId,
        String userNickname,
        String content,
        Instant createdAt
) {
}
