package com.sprint.mission.sb15monewteam02.comment.repository;

import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;

import java.util.List;
import java.util.UUID;

public interface CommentRepositoryCustom {

  List<Comment> findByCursor(
      UUID articleId,
      CommentOrderBy orderBy,
      SortDirection direction,
      String cursor,
      String after,
      int limit
  );
}
