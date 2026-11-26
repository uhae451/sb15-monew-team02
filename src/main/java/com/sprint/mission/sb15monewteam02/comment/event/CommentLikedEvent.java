package com.sprint.mission.sb15monewteam02.comment.event;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
        UUID commentLikeId,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID likedBy,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt,
        Instant likedAt
) {
}
