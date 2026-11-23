package com.sprint.mission.sb15monewteam02.comment.service;

import com.sprint.mission.sb15monewteam02.comment.dto.CommentLikeResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CommentResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CommentSearchCondition;
import com.sprint.mission.sb15monewteam02.comment.dto.CreateCommentRequest;
import com.sprint.mission.sb15monewteam02.comment.dto.UpdateCommentRequest;
import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentLike;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.comment.event.CommentCreatedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentDeletedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentHardDeletedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentLikedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentUnlikedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentUpdatedEvent;
import com.sprint.mission.sb15monewteam02.comment.exception.ArticleNotFoundException;
import com.sprint.mission.sb15monewteam02.comment.exception.CommentForbiddenException;
import com.sprint.mission.sb15monewteam02.comment.exception.CommentLikeNotFoundException;
import com.sprint.mission.sb15monewteam02.comment.exception.CommentNotFoundException;
import com.sprint.mission.sb15monewteam02.comment.exception.DuplicateCommentLikeException;
import com.sprint.mission.sb15monewteam02.comment.exception.UserNotFoundException;
import com.sprint.mission.sb15monewteam02.comment.mapper.CommentMapper;
import com.sprint.mission.sb15monewteam02.comment.port.ArticleReader;
import com.sprint.mission.sb15monewteam02.comment.port.ArticleSummary;
import com.sprint.mission.sb15monewteam02.comment.port.CommentEventPublisher;
import com.sprint.mission.sb15monewteam02.comment.port.UserReader;
import com.sprint.mission.sb15monewteam02.comment.port.UserSummary;
import com.sprint.mission.sb15monewteam02.comment.repository.CommentLikeRepository;
import com.sprint.mission.sb15monewteam02.comment.repository.CommentRepository;
import com.sprint.mission.sb15monewteam02.common.dto.CursorPageResponse;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private ArticleReader articleReader;

    @Mock
    private CommentEventPublisher eventPublisher;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository,
                commentLikeRepository,
                userReader,
                articleReader,
                eventPublisher,
                new CommentMapper()
        );
    }

    @Test
    @DisplayName("댓글 등록 시 기사·사용자 존재를 확인하고 저장 후 이벤트를 발행한다")
    void createCommentSuccess() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(articleId, userId, "좋은 기사네요.");

        given(articleReader.findActiveArticle(articleId))
                .willReturn(Optional.of(new ArticleSummary(articleId, "뉴스 제목")));
        given(userReader.findActiveUser(userId))
                .willReturn(Optional.of(new UserSummary(userId, "테스터")));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(comment, "createdAt", Instant.now());
            return comment;
        });

        CommentResponse response = commentService.createComment(request);

        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.userNickname()).isEqualTo("테스터");
        assertThat(response.content()).isEqualTo("좋은 기사네요.");
        assertThat(response.likeCount()).isZero();
        assertThat(response.likedByMe()).isFalse();
        verify(eventPublisher).publishCommentCreated(any(CommentCreatedEvent.class));
    }

    @Test
    @DisplayName("존재하지 않는 기사에 댓글을 등록하면 ArticleNotFoundException이 발생한다")
    void createCommentFailsWhenArticleNotFound() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(articleReader.findActiveArticle(articleId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(new CreateCommentRequest(articleId, userId, "내용")))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 댓글을 등록하면 UserNotFoundException이 발생한다")
    void createCommentFailsWhenUserNotFound() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(articleReader.findActiveArticle(articleId))
                .willReturn(Optional.of(new ArticleSummary(articleId, "뉴스 제목")));
        given(userReader.findActiveUser(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(new CreateCommentRequest(articleId, userId, "내용")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 목록 조회 시 createdAt 및 likeCount 기준 커서와 likedByMe를 정확히 반환한다")
    void getCommentsReturnsCursorPageWithLikedByMe() {
        UUID articleId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();

        Instant t1 = Instant.parse("2026-11-23T10:00:00Z");
        Instant t2 = Instant.parse("2026-11-23T10:01:00Z");
        Instant t3 = Instant.parse("2026-11-23T10:02:00Z");

        Comment c1 = Comment.createWithMetrics(articleId, user1, "첫번째", 10L, t1);
        ReflectionTestUtils.setField(c1, "id", UUID.randomUUID());
        Comment c2 = Comment.createWithMetrics(articleId, user2, "두번째", 5L, t2);
        ReflectionTestUtils.setField(c2, "id", UUID.randomUUID());
        Comment c3 = Comment.createWithMetrics(articleId, user1, "세번째", 1L, t3);
        ReflectionTestUtils.setField(c3, "id", UUID.randomUUID());

        CommentSearchCondition condition = new CommentSearchCondition(
                articleId, CommentOrderBy.LIKE_COUNT, SortDirection.DESC, null, null, 2
        );

        given(articleReader.findActiveArticle(articleId))
                .willReturn(Optional.of(new ArticleSummary(articleId, "기사")));
        given(commentRepository.findByCursor(articleId, CommentOrderBy.LIKE_COUNT, SortDirection.DESC, null, null, 2))
                .willReturn(List.of(c1, c2, c3));
        given(commentRepository.countByArticleIdAndDeletedFalse(articleId)).willReturn(3L);
        given(commentLikeRepository.findLikedCommentIds(List.of(c1.getId(), c2.getId()), viewerId))
                .willReturn(Set.of(c2.getId()));
        given(userReader.findUsersByIds(any())).willReturn(Map.of(
                user1, new UserSummary(user1, "유저1"),
                user2, new UserSummary(user2, "유저2")
        ));

        CursorPageResponse<CommentResponse> page = commentService.getComments(condition, viewerId);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo("5");
        assertThat(page.nextAfter()).isEqualTo(t2.toString());
        assertThat(page.totalElements()).isEqualTo(3L);
        assertThat(page.content().get(0).likedByMe()).isFalse();
        assertThat(page.content().get(1).likedByMe()).isTrue();
        assertThat(page.content().get(0).userNickname()).isEqualTo("유저1");
    }

    @Test
    @DisplayName("마지막 페이지 조회 시 hasNext는 false이고 nextCursor와 nextAfter는 null이다")
    void getCommentsLastPageHasNullCursors() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-11-23T10:00:00Z");
        Comment c1 = Comment.createWithMetrics(articleId, userId, "유일한 댓글", 0L, t1);
        ReflectionTestUtils.setField(c1, "id", UUID.randomUUID());

        CommentSearchCondition condition = new CommentSearchCondition(
                articleId, CommentOrderBy.CREATED_AT, SortDirection.ASC, null, null, 5
        );

        given(articleReader.findActiveArticle(articleId))
                .willReturn(Optional.of(new ArticleSummary(articleId, "기사")));
        given(commentRepository.findByCursor(articleId, CommentOrderBy.CREATED_AT, SortDirection.ASC, null, null, 5))
                .willReturn(List.of(c1));
        given(commentRepository.countByArticleIdAndDeletedFalse(articleId)).willReturn(1L);
        given(userReader.findUsersByIds(any())).willReturn(Map.of(userId, new UserSummary(userId, "작성자")));

        CursorPageResponse<CommentResponse> page = commentService.getComments(condition, null);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.nextAfter()).isNull();
        assertThat(page.content().get(0).likedByMe()).isFalse();
        verify(commentLikeRepository, never()).findLikedCommentIds(any(), any());
    }

    @Test
    @DisplayName("본인이 작성한 댓글만 수정할 수 있으며 타인이 수정하려 하면 CommentForbiddenException이 발생한다")
    void updateCommentAuthorization() {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        Comment comment = Comment.createWithMetrics(articleId, authorId, "원본", 3L, Instant.now());
        ReflectionTestUtils.setField(comment, "id", commentId);

        given(commentRepository.findByIdAndDeletedFalse(commentId)).willReturn(Optional.of(comment));
        given(userReader.findActiveUser(authorId)).willReturn(Optional.of(new UserSummary(authorId, "작성자")));
        given(commentLikeRepository.existsByCommentIdAndUserId(commentId, authorId)).willReturn(true);

        CommentResponse updated = commentService.updateComment(commentId, authorId, new UpdateCommentRequest("수정됨"));
        assertThat(updated.content()).isEqualTo("수정됨");
        assertThat(updated.likedByMe()).isTrue();
        verify(eventPublisher).publishCommentUpdated(any(CommentUpdatedEvent.class));

        assertThatThrownBy(() -> commentService.updateComment(commentId, otherUserId, new UpdateCommentRequest("탈취")))
                .isInstanceOf(CommentForbiddenException.class);
    }

    @Test
    @DisplayName("댓글 논리 삭제 시 deleted 상태만 변경되고 좋아요는 유지된다")
    void softDeleteCommentMarksDeletedOnly() {
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.createWithMetrics(UUID.randomUUID(), authorId, "삭제 대상", 2L, Instant.now());
        ReflectionTestUtils.setField(comment, "id", commentId);

        given(commentRepository.findByIdAndDeletedFalse(commentId)).willReturn(Optional.of(comment));

        commentService.softDeleteComment(commentId, authorId);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isNotNull();
        verify(commentLikeRepository, never()).deleteAllByCommentId(any());
        verify(commentRepository, never()).delete(any());
        verify(eventPublisher).publishCommentDeleted(any(CommentDeletedEvent.class));
    }

    @Test
    @DisplayName("댓글 물리 삭제 시 논리 삭제된 댓글도 포함해 조회하고 연관 좋아요와 댓글을 모두 삭제한다")
    void hardDeleteCommentRemovesLikesAndComment() {
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.createWithMetrics(UUID.randomUUID(), authorId, "물리 삭제 대상", 2L, Instant.now());
        ReflectionTestUtils.setField(comment, "id", commentId);
        comment.softDelete();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        commentService.hardDeleteComment(commentId);

        verify(commentLikeRepository).deleteAllByCommentId(commentId);
        verify(commentRepository).delete(comment);
        verify(eventPublisher).publishCommentHardDeleted(any(CommentHardDeletedEvent.class));
    }

    @Test
    @DisplayName("좋아요 등록 성공 시 CommentLike 저장 및 원자적 likeCount 증가 후 상세 응답을 반환한다")
    void likeCommentSuccess() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();

        Comment comment = Comment.createWithMetrics(articleId, authorId, "좋아요 테스트", 0L, Instant.now());
        ReflectionTestUtils.setField(comment, "id", commentId);
        Comment incremented = Comment.createWithMetrics(articleId, authorId, "좋아요 테스트", 1L, comment.getCreatedAt());
        ReflectionTestUtils.setField(incremented, "id", commentId);

        given(userReader.findActiveUser(likerId)).willReturn(Optional.of(new UserSummary(likerId, "좋아요유저")));
        given(commentRepository.findByIdAndDeletedFalse(commentId)).willReturn(Optional.of(comment), Optional.of(incremented));
        given(commentLikeRepository.existsByCommentIdAndUserId(commentId, likerId)).willReturn(false);
        given(commentLikeRepository.saveAndFlush(any(CommentLike.class))).willAnswer(invocation -> {
            CommentLike like = invocation.getArgument(0);
            ReflectionTestUtils.setField(like, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(like, "createdAt", Instant.now());
            return like;
        });
        given(commentRepository.incrementLikeCount(commentId)).willReturn(1);
        given(articleReader.findActiveArticle(articleId)).willReturn(Optional.of(new ArticleSummary(articleId, "기사 제목")));
        given(userReader.findActiveUser(authorId)).willReturn(Optional.of(new UserSummary(authorId, "댓글작성자")));

        CommentLikeResponse response = commentService.likeComment(commentId, likerId);

        assertThat(response.likedBy()).isEqualTo(likerId);
        assertThat(response.commentId()).isEqualTo(commentId);
        assertThat(response.articleTitle()).isEqualTo("기사 제목");
        assertThat(response.commentUserNickname()).isEqualTo("댓글작성자");
        assertThat(response.commentLikeCount()).isEqualTo(1L);
        verify(eventPublisher).publishCommentLiked(any(CommentLikedEvent.class));
    }

    @Test
    @DisplayName("이미 좋아요한 댓글이거나 유니크 제약조건 충돌 시 DuplicateCommentLikeException을 던진다")
    void duplicateLikeThrowsException() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = Comment.createWithMetrics(UUID.randomUUID(), UUID.randomUUID(), "댓글", 1L, Instant.now());
        ReflectionTestUtils.setField(comment, "id", commentId);

        given(userReader.findActiveUser(userId)).willReturn(Optional.of(new UserSummary(userId, "유저")));
        given(commentRepository.findByIdAndDeletedFalse(commentId)).willReturn(Optional.of(comment));
        given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(true);

        assertThatThrownBy(() -> commentService.likeComment(commentId, userId))
                .isInstanceOf(DuplicateCommentLikeException.class);
        verify(commentRepository, never()).incrementLikeCount(any());

        given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(false);
        given(commentLikeRepository.saveAndFlush(any(CommentLike.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> commentService.likeComment(commentId, userId))
                .isInstanceOf(DuplicateCommentLikeException.class);
        verify(commentRepository, never()).incrementLikeCount(any());
    }

    @Test
    @DisplayName("좋아요 취소 시 삭제된 행이 있으면 likeCount를 감소시키고, 없으면 CommentLikeNotFoundException을 던진다")
    void unlikeCommentSuccessAndNotFound() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = Comment.createWithMetrics(UUID.randomUUID(), UUID.randomUUID(), "댓글", 1L, Instant.now());
        ReflectionTestUtils.setField(comment, "id", commentId);

        given(commentRepository.findByIdAndDeletedFalse(commentId)).willReturn(Optional.of(comment));
        given(commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId)).willReturn(1);

        commentService.unlikeComment(commentId, userId);

        verify(commentRepository).decrementLikeCount(commentId);
        verify(eventPublisher).publishCommentUnliked(any(CommentUnlikedEvent.class));

        given(commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId)).willReturn(0);
        assertThatThrownBy(() -> commentService.unlikeComment(commentId, userId))
                .isInstanceOf(CommentLikeNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 조회/수정/삭제 시 CommentNotFoundException이 발생한다")
    void throwsCommentNotFoundWhenMissing() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(commentRepository.findByIdAndDeletedFalse(commentId)).willReturn(Optional.empty());
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(commentId, userId, new UpdateCommentRequest("수정")))
                .isInstanceOf(CommentNotFoundException.class);
        assertThatThrownBy(() -> commentService.softDeleteComment(commentId, userId))
                .isInstanceOf(CommentNotFoundException.class);
        assertThatThrownBy(() -> commentService.hardDeleteComment(commentId))
                .isInstanceOf(CommentNotFoundException.class);
    }
}
