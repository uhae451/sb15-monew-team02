package com.sprint.mission.sb15monewteam02.comment.dto;

import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;

import java.util.UUID;

public record CommentSearchCondition(
        UUID articleId,
        CommentOrderBy orderBy,
        SortDirection direction,
        String cursor,
        String after,
        int limit
) {
}
