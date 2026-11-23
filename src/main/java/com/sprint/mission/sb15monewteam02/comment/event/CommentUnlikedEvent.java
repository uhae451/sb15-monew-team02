package com.sprint.mission.sb15monewteam02.comment.event;

import java.util.UUID;

public record CommentUnlikedEvent(
        UUID commentId,
        UUID userId
) {
}
