package com.sprint.mission.sb15monewteam02.comment.event;

import java.util.UUID;

public record CommentHardDeletedEvent(
        UUID commentId,
        UUID articleId,
        UUID userId
) {
}
