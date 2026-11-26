package com.sprint.mission.sb15monewteam02.comment.repository;

import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

  Optional<Comment> findByIdAndDeletedFalse(UUID id);

  long countByArticleIdAndDeletedFalse(UUID articleId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1, c.updatedAt = :now WHERE c.id = :commentId AND c.deleted = false")
  int incrementLikeCount(@Param("commentId") UUID commentId, @Param("now") Instant now);

  default int incrementLikeCount(UUID commentId) {
    return incrementLikeCount(commentId, Instant.now());
  }

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE Comment c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END, c.updatedAt = :now WHERE c.id = :commentId AND c.deleted = false")
  int decrementLikeCount(@Param("commentId") UUID commentId, @Param("now") Instant now);

  default int decrementLikeCount(UUID commentId) {
    return decrementLikeCount(commentId, Instant.now());
  }
}
