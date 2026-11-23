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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserReader userReader;
    private final ArticleReader articleReader;
    private final CommentEventPublisher eventPublisher;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request) {
        articleReader.findActiveArticle(request.articleId())
                .orElseThrow(() -> new ArticleNotFoundException(request.articleId()));
        UserSummary user = userReader.findActiveUser(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Comment saved = commentRepository.save(
                Comment.create(request.articleId(), request.userId(), request.content().trim())
        );

        eventPublisher.publishCommentCreated(new CommentCreatedEvent(
                saved.getId(),
                saved.getArticleId(),
                saved.getUserId(),
                user.nickname(),
                saved.getContent(),
                saved.getCreatedAt()
        ));

        return commentMapper.toResponse(saved, user.nickname(), false);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponse> getComments(CommentSearchCondition condition, UUID requestUserId) {
        articleReader.findActiveArticle(condition.articleId())
                .orElseThrow(() -> new ArticleNotFoundException(condition.articleId()));

        CommentOrderBy orderBy = condition.orderBy() != null ? condition.orderBy() : CommentOrderBy.CREATED_AT;
        SortDirection direction = condition.direction() != null ? condition.direction() : SortDirection.DESC;
        int limit = condition.limit() > 0 ? condition.limit() : 50;

        List<Comment> fetched = commentRepository.findByCursor(
                condition.articleId(),
                orderBy,
                direction,
                condition.cursor(),
                condition.after(),
                limit
        );

        boolean hasNext = fetched.size() > limit;
        List<Comment> pageComments = hasNext ? fetched.subList(0, limit) : fetched;
        long totalElements = commentRepository.countByArticleIdAndDeletedFalse(condition.articleId());

        Set<UUID> likedCommentIds = resolveLikedCommentIds(pageComments, requestUserId);
        Map<UUID, UserSummary> usersById = resolveUsers(pageComments);

        List<CommentResponse> content = pageComments.stream()
                .map(comment -> {
                    String nickname = usersById.getOrDefault(
                            comment.getUserId(),
                            new UserSummary(comment.getUserId(), "알 수 없음")
                    ).nickname();
                    return commentMapper.toResponse(comment, nickname, likedCommentIds.contains(comment.getId()));
                })
                .toList();

        String nextCursor = null;
        String nextAfter = null;
        if (hasNext && !pageComments.isEmpty()) {
            Comment last = pageComments.get(pageComments.size() - 1);
            nextCursor = orderBy == CommentOrderBy.LIKE_COUNT
                    ? String.valueOf(last.getLikeCount())
                    : last.getCreatedAt().toString();
            nextAfter = last.getCreatedAt().toString();
        }

        return CursorPageResponse.of(content, nextCursor, nextAfter, totalElements, hasNext);
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UUID requestUserId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getUserId().equals(requestUserId)) {
            throw new CommentForbiddenException();
        }

        comment.updateContent(request.content().trim());

        eventPublisher.publishCommentUpdated(new CommentUpdatedEvent(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getUpdatedAt()
        ));

        String nickname = userReader.findActiveUser(comment.getUserId())
                .map(UserSummary::nickname)
                .orElse("알 수 없음");
        boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId);

        return commentMapper.toResponse(comment, nickname, likedByMe);
    }

    @Transactional
    public void softDeleteComment(UUID commentId, UUID requestUserId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (requestUserId != null && !comment.getUserId().equals(requestUserId)) {
            throw new CommentForbiddenException();
        }

        comment.softDelete();

        eventPublisher.publishCommentDeleted(new CommentDeletedEvent(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                comment.getDeletedAt()
        ));
    }

    @Transactional
    public void hardDeleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        commentLikeRepository.deleteAllByCommentId(commentId);
        commentRepository.delete(comment);

        eventPublisher.publishCommentHardDeleted(new CommentHardDeletedEvent(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId()
        ));
    }

    @Transactional
    public CommentLikeResponse likeComment(UUID commentId, UUID requestUserId) {
        userReader.findActiveUser(requestUserId)
                .orElseThrow(() -> new UserNotFoundException(requestUserId));

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId)) {
            throw new DuplicateCommentLikeException();
        }

        CommentLike savedLike;
        try {
            savedLike = commentLikeRepository.saveAndFlush(CommentLike.create(comment, requestUserId));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCommentLikeException();
        }

        int updatedRows = commentRepository.incrementLikeCount(commentId);
        if (updatedRows == 0) {
            throw new CommentNotFoundException(commentId);
        }

        Comment refreshedComment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElse(comment);

        String articleTitle = articleReader.findActiveArticle(refreshedComment.getArticleId())
                .map(ArticleSummary::title)
                .orElse("");
        String commentUserNickname = userReader.findActiveUser(refreshedComment.getUserId())
                .map(UserSummary::nickname)
                .orElse("알 수 없음");

        eventPublisher.publishCommentLiked(new CommentLikedEvent(
                savedLike.getId(),
                refreshedComment.getId(),
                refreshedComment.getArticleId(),
                articleTitle,
                requestUserId,
                refreshedComment.getUserId(),
                commentUserNickname,
                refreshedComment.getContent(),
                refreshedComment.getLikeCount(),
                refreshedComment.getCreatedAt(),
                savedLike.getCreatedAt()
        ));

        return commentMapper.toLikeResponse(
                savedLike,
                refreshedComment,
                articleTitle,
                commentUserNickname,
                refreshedComment.getLikeCount()
        );
    }

    @Transactional
    public void unlikeComment(UUID commentId, UUID requestUserId) {
        commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        int deletedRows = commentLikeRepository.deleteByCommentIdAndUserId(commentId, requestUserId);
        if (deletedRows == 0) {
            throw new CommentLikeNotFoundException();
        }

        commentRepository.decrementLikeCount(commentId);
        eventPublisher.publishCommentUnliked(new CommentUnlikedEvent(commentId, requestUserId));
    }

    private Set<UUID> resolveLikedCommentIds(List<Comment> pageComments, UUID requestUserId) {
        if (requestUserId == null || pageComments.isEmpty()) {
            return Set.of();
        }
        List<UUID> commentIds = pageComments.stream().map(Comment::getId).toList();
        return commentLikeRepository.findLikedCommentIds(commentIds, requestUserId);
    }

    private Map<UUID, UserSummary> resolveUsers(List<Comment> pageComments) {
        if (pageComments.isEmpty()) {
            return Map.of();
        }
        List<UUID> userIds = pageComments.stream().map(Comment::getUserId).distinct().toList();
        return userReader.findUsersByIds(userIds);
    }
}
