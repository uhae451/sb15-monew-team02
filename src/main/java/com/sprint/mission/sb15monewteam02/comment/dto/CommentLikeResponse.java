package com.sprint.mission.sb15monewteam02.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeResponse(
        UUID id,
        UUID likedBy,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt
) {
}
