package com.sprint.mission.sb15monewteam02.comment.repository;

import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentLike;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.comment.exception.InvalidCursorException;
import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("논리 삭제된 댓글은 활성 조회 및 개수 집계에서 제외되지만 물리 조회에는 존재한다")
  void softDeletedCommentIsExcludedFromActiveQueries() {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Comment active = commentRepository.save(Comment.create(articleId, userId, "활성 댓글"));
    Comment deleted = commentRepository.save(Comment.create(articleId, userId, "삭제될 댓글"));
    deleted.softDelete();
    entityManager.flush();
    entityManager.clear();

    assertThat(commentRepository.findByIdAndDeletedFalse(active.getId())).isPresent();
    assertThat(commentRepository.findByIdAndDeletedFalse(deleted.getId())).isEmpty();
    assertThat(commentRepository.findById(deleted.getId())).isPresent();
    assertThat(commentRepository.countByArticleIdAndDeletedFalse(articleId)).isEqualTo(1L);
  }

  @Test
  @DisplayName("댓글 물리 삭제 시 연관된 좋아요 정보도 모두 삭제된다")
  void hardDeleteRemovesCommentAndRelatedLikes() {
    UUID articleId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    UUID liker1 = UUID.randomUUID();
    UUID liker2 = UUID.randomUUID();

    Comment comment = commentRepository.save(Comment.create(articleId, authorId, "물리 삭제 대상"));
    commentLikeRepository.save(CommentLike.create(comment, liker1));
    commentLikeRepository.save(CommentLike.create(comment, liker2));
    entityManager.flush();
    entityManager.clear();

    commentLikeRepository.deleteAllByCommentId(comment.getId());
    commentRepository.deleteById(comment.getId());
    entityManager.flush();
    entityManager.clear();

    assertThat(commentRepository.findById(comment.getId())).isEmpty();
    assertThat(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), liker1)).isFalse();
    assertThat(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), liker2)).isFalse();
  }

  @Test
  @DisplayName("동일 사용자가 같은 댓글에 좋아요를 중복 저장하면 유니크 제약조건 예외가 발생한다")
  void duplicateCommentLikeThrowsDataIntegrityViolation() {
    Comment comment = commentRepository.save(Comment.create(UUID.randomUUID(), UUID.randomUUID(), "댓글"));
    UUID userId = UUID.randomUUID();

    commentLikeRepository.saveAndFlush(CommentLike.create(comment, userId));

    assertThatThrownBy(() -> {
      commentLikeRepository.saveAndFlush(CommentLike.create(comment, userId));
    }).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("원자적 좋아요 증감 및 삭제 쿼리가 정확히 동작한다")
  void atomicLikeCountIncrementDecrementAndDelete() {
    Comment comment = commentRepository.saveAndFlush(Comment.create(UUID.randomUUID(), UUID.randomUUID(), "댓글"));
    UUID userId = UUID.randomUUID();
    commentLikeRepository.saveAndFlush(CommentLike.create(comment, userId));

    int inc = commentRepository.incrementLikeCount(comment.getId());
    entityManager.clear();
    assertThat(inc).isEqualTo(1);
    assertThat(commentRepository.findById(comment.getId()).orElseThrow().getLikeCount()).isEqualTo(1L);

    int deletedCount = commentLikeRepository.deleteByCommentIdAndUserId(comment.getId(), userId);
    int secondDeleteCount = commentLikeRepository.deleteByCommentIdAndUserId(comment.getId(), userId);
    int dec = commentRepository.decrementLikeCount(comment.getId());
    int decFloor = commentRepository.decrementLikeCount(comment.getId());
    entityManager.clear();

    assertThat(deletedCount).isEqualTo(1);
    assertThat(secondDeleteCount).isEqualTo(0);
    assertThat(dec).isEqualTo(1);
    assertThat(decFloor).isEqualTo(1);
    assertThat(commentRepository.findById(comment.getId()).orElseThrow().getLikeCount()).isEqualTo(0L);
  }

  @Test
  @DisplayName("createdAt 오름차순 및 내림차순 커서 페이지네이션이 정확히 동작한다")
  void cursorPaginationByCreatedAtAscAndDesc() {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant base = Instant.parse("2026-11-23T10:00:00Z");

    Comment c1 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "c1", 0L, base));
    Comment c2 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "c2", 0L, base.plusSeconds(10)));
    Comment c3 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "c3", 0L, base.plusSeconds(20)));
    Comment deleted = commentRepository.save(Comment.createWithMetrics(articleId, userId, "deleted", 0L, base.plusSeconds(30)));
    deleted.softDelete();
    entityManager.flush();
    entityManager.clear();

    // ASC page 1 (limit=2 -> fetch 3)
    List<Comment> ascPage1 = commentRepository.findByCursor(
        articleId, CommentOrderBy.CREATED_AT, SortDirection.ASC, null, null, 2
    );
    assertThat(ascPage1).extracting(Comment::getId).containsExactly(c1.getId(), c2.getId(), c3.getId());

    // ASC page 2 using c2 createdAt cursor
    List<Comment> ascPage2 = commentRepository.findByCursor(
        articleId, CommentOrderBy.CREATED_AT, SortDirection.ASC, c2.getCreatedAt().toString(), c2.getCreatedAt().toString(), 2
    );
    assertThat(ascPage2).extracting(Comment::getId).containsExactly(c3.getId());

    // DESC page 1 & page 2
    List<Comment> descPage1 = commentRepository.findByCursor(
        articleId, CommentOrderBy.CREATED_AT, SortDirection.DESC, null, null, 2
    );
    assertThat(descPage1).extracting(Comment::getId).containsExactly(c3.getId(), c2.getId(), c1.getId());

    List<Comment> descPage2 = commentRepository.findByCursor(
        articleId, CommentOrderBy.CREATED_AT, SortDirection.DESC, c2.getCreatedAt().toString(), c2.getCreatedAt().toString(), 2
    );
    assertThat(descPage2).extracting(Comment::getId).containsExactly(c1.getId());
  }

  @Test
  @DisplayName("likeCount 정렬 시 동점자 처리를 위해 after(createdAt) 보조 커서로 페이지네이션한다")
  void cursorPaginationByLikeCountWithAfterTieBreaker() {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant base = Instant.parse("2026-11-23T10:00:00Z");

    Comment c1 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "like5-old", 5L, base));
    Comment c2 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "like5-new", 5L, base.plusSeconds(10)));
    Comment c3 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "like10", 10L, base.plusSeconds(20)));
    Comment c4 = commentRepository.save(Comment.createWithMetrics(articleId, userId, "like1", 1L, base.plusSeconds(30)));
    entityManager.flush();
    entityManager.clear();

    // DESC order should be: c3(10), c2(5, +10s), c1(5, +0s), c4(1)
    List<Comment> descPage1 = commentRepository.findByCursor(
        articleId, CommentOrderBy.LIKE_COUNT, SortDirection.DESC, null, null, 2
    );
    assertThat(descPage1).hasSize(3);
    assertThat(descPage1.subList(0, 2)).extracting(Comment::getId).containsExactly(c3.getId(), c2.getId());

    // Next page after c2 (likeCount=5, createdAt=base+10s) -> must return c1(5, base), c4(1)
    List<Comment> descPage2 = commentRepository.findByCursor(
        articleId,
        CommentOrderBy.LIKE_COUNT,
        SortDirection.DESC,
        String.valueOf(c2.getLikeCount()),
        c2.getCreatedAt().toString(),
        2
    );
    assertThat(descPage2).extracting(Comment::getId).containsExactly(c1.getId(), c4.getId());

    // ASC order should be: c4(1), c1(5, +0s), c2(5, +10s), c3(10)
    List<Comment> ascPage1 = commentRepository.findByCursor(
        articleId, CommentOrderBy.LIKE_COUNT, SortDirection.ASC, null, null, 2
    );
    assertThat(ascPage1.subList(0, 2)).extracting(Comment::getId).containsExactly(c4.getId(), c1.getId());

    List<Comment> ascPage2 = commentRepository.findByCursor(
        articleId,
        CommentOrderBy.LIKE_COUNT,
        SortDirection.ASC,
        String.valueOf(c1.getLikeCount()),
        c1.getCreatedAt().toString(),
        2
    );
    assertThat(ascPage2).extracting(Comment::getId).containsExactly(c2.getId(), c3.getId());
  }

  @Test
  @DisplayName("특정 사용자가 좋아요한 댓글 ID 목록을 한 번에 조회한다")
  void findLikedCommentIdsBatch() {
    UUID articleId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    UUID viewerId = UUID.randomUUID();

    Comment c1 = commentRepository.save(Comment.create(articleId, authorId, "1"));
    Comment c2 = commentRepository.save(Comment.create(articleId, authorId, "2"));
    commentLikeRepository.save(CommentLike.create(c1, viewerId));
    entityManager.flush();
    entityManager.clear();

    Set<UUID> likedIds = commentLikeRepository.findLikedCommentIds(List.of(c1.getId(), c2.getId()), viewerId);
    assertThat(likedIds).containsExactly(c1.getId());
  }

  @Test
  @DisplayName("잘못된 커서 형식이 전달되면 InvalidCursorException이 발생한다")
  void invalidCursorThrowsException() {
    UUID articleId = UUID.randomUUID();

    assertThatThrownBy(() -> commentRepository.findByCursor(
        articleId, CommentOrderBy.CREATED_AT, SortDirection.DESC, "not-an-instant", null, 5
    )).isInstanceOf(InvalidCursorException.class);

    assertThatThrownBy(() -> commentRepository.findByCursor(
        articleId, CommentOrderBy.LIKE_COUNT, SortDirection.DESC, "not-a-number", null, 5
    )).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("CommentOrderBy는 문자열 값을 대소문자 구분 없이 파싱하고 잘못된 값은 예외를 던진다")
  void commentOrderByParsing() {
    assertThat(CommentOrderBy.from(null)).isEqualTo(CommentOrderBy.CREATED_AT);
    assertThat(CommentOrderBy.from("likeCount")).isEqualTo(CommentOrderBy.LIKE_COUNT);
    assertThat(CommentOrderBy.from("CREATED_AT")).isEqualTo(CommentOrderBy.CREATED_AT);
    assertThatThrownBy(() -> CommentOrderBy.from("invalid"))
        .isInstanceOf(BusinessException.class);
  }
}
