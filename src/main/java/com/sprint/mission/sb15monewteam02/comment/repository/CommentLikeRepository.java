package com.sprint.mission.sb15monewteam02.comment.repository;

import com.sprint.mission.sb15monewteam02.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment.id IN :commentIds AND cl.userId = :userId")
    Set<UUID> findLikedCommentIds(@Param("commentIds") Collection<UUID> commentIds, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.userId = :userId")
    int deleteByCommentIdAndUserId(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    int deleteAllByCommentId(@Param("commentId") UUID commentId);
}
