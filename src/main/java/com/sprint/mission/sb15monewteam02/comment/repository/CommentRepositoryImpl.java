package com.sprint.mission.sb15monewteam02.comment.repository;

import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.comment.exception.InvalidCursorException;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommentRepositoryImpl implements CommentRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<Comment> findByCursor(
      UUID articleId,
      CommentOrderBy orderBy,
      SortDirection direction,
      String cursor,
      String after,
      int limit
  ) {
    CommentOrderBy resolvedOrderBy = orderBy != null ? orderBy : CommentOrderBy.CREATED_AT;
    SortDirection resolvedDirection = direction != null ? direction : SortDirection.DESC;
    boolean ascending = resolvedDirection == SortDirection.ASC;

    StringBuilder jpql = new StringBuilder(
        "SELECT c FROM Comment c WHERE c.articleId = :articleId AND c.deleted = false"
    );
    Map<String, Object> params = new HashMap<>();
    params.put("articleId", articleId);

    if (resolvedOrderBy == CommentOrderBy.CREATED_AT) {
      appendCreatedAtCursor(jpql, params, cursor, after, ascending);
    } else {
      appendLikeCountCursor(jpql, params, cursor, after, ascending);
    }

    TypedQuery<Comment> query = entityManager.createQuery(jpql.toString(), Comment.class);
    params.forEach(query::setParameter);
    query.setMaxResults(Math.max(1, limit) + 1);
    return query.getResultList();
  }

  private void appendCreatedAtCursor(
      StringBuilder jpql,
      Map<String, Object> params,
      String cursor,
      String after,
      boolean ascending
  ) {
    Instant cursorInstant = parseInstant(StringUtils.hasText(cursor) ? cursor : after);
    if (cursorInstant != null) {
      jpql.append(ascending ? " AND c.createdAt > :cursorInstant" : " AND c.createdAt < :cursorInstant");
      params.put("cursorInstant", cursorInstant);
    }
    jpql.append(ascending ? " ORDER BY c.createdAt ASC, c.id ASC" : " ORDER BY c.createdAt DESC, c.id DESC");
  }

  private void appendLikeCountCursor(
      StringBuilder jpql,
      Map<String, Object> params,
      String cursor,
      String after,
      boolean ascending
  ) {
    Long cursorLikeCount = parseLong(cursor);
    Instant afterInstant = parseInstant(after);
    if (cursorLikeCount != null) {
      if (afterInstant != null) {
        jpql.append(ascending
            ? " AND (c.likeCount > :cursorLikeCount OR (c.likeCount = :cursorLikeCount AND c.createdAt > :afterInstant))"
            : " AND (c.likeCount < :cursorLikeCount OR (c.likeCount = :cursorLikeCount AND c.createdAt < :afterInstant))");
        params.put("afterInstant", afterInstant);
      } else {
        jpql.append(ascending ? " AND c.likeCount > :cursorLikeCount" : " AND c.likeCount < :cursorLikeCount");
      }
      params.put("cursorLikeCount", cursorLikeCount);
    }
    jpql.append(ascending
        ? " ORDER BY c.likeCount ASC, c.createdAt ASC, c.id ASC"
        : " ORDER BY c.likeCount DESC, c.createdAt DESC, c.id DESC");
  }

  private Instant parseInstant(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    try {
      return Instant.parse(raw.trim());
    } catch (DateTimeParseException ex) {
      throw new InvalidCursorException();
    }
  }

  private Long parseLong(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException ex) {
      throw new InvalidCursorException();
    }
  }
}
