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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

  private static final int DEFAULT_PAGE_LIMIT = 50;
  private static final String UNKNOWN_NICKNAME = "알 수 없음";

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final UserReader userReader;
  private final ArticleReader articleReader;
  private final CommentEventPublisher eventPublisher;
  private final CommentMapper commentMapper;

  @Transactional
  public CommentResponse createComment(CreateCommentRequest request) {
    articleReader.findActiveArticle(request.articleId())
        .orElseThrow(() -> new ArticleNotFoundException());
    UserSummary user = userReader.findActiveUser(request.userId())
        .orElseThrow(() -> new UserNotFoundException());

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

    log.info("Comment created: commentId={}, articleId={}, userId={}",
        saved.getId(), saved.getArticleId(), saved.getUserId());
    return commentMapper.toResponse(saved, user.nickname(), false);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<CommentResponse> getComments(CommentSearchCondition condition, UUID requestUserId) {
    if (condition.articleId() != null) {
      articleReader.findActiveArticle(condition.articleId())
          .orElseThrow(() -> new ArticleNotFoundException());
    }


    CommentOrderBy orderBy = condition.orderBy() != null ? condition.orderBy() : CommentOrderBy.CREATED_AT;
    SortDirection direction = condition.direction() != null ? condition.direction() : SortDirection.DESC;
    int limit = condition.limit() > 0 ? condition.limit() : DEFAULT_PAGE_LIMIT;

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

    List<CommentResponse> content = mapToResponses(pageComments, requestUserId);
    CursorTokens cursorTokens = extractCursorTokens(pageComments, orderBy, hasNext);

    return new CursorPageResponse<>(
        content,
        cursorTokens.nextCursor(),
        cursorTokens.nextAfter(),
        content.size(),
        totalElements,
        hasNext
    );
  }

  @Transactional
  public CommentResponse updateComment(UUID commentId, UUID requestUserId, UpdateCommentRequest request) {
    Comment comment = findActiveComment(commentId);
    validateAuthor(comment, requestUserId);

    comment.updateContent(request.content().trim());

    eventPublisher.publishCommentUpdated(new CommentUpdatedEvent(
        comment.getId(),
        comment.getArticleId(),
        comment.getUserId(),
        comment.getContent(),
        comment.getUpdatedAt()
    ));

    String nickname = resolveNickname(comment.getUserId());
    boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId);

    log.info("Comment updated: commentId={}, userId={}", commentId, requestUserId);
    return commentMapper.toResponse(comment, nickname, likedByMe);
  }

  @Transactional
  public void softDeleteComment(UUID commentId, UUID requestUserId) {
    Comment comment = findActiveComment(commentId);
    if (requestUserId != null) {
      validateAuthor(comment, requestUserId);
    }

    comment.softDelete();

    eventPublisher.publishCommentDeleted(new CommentDeletedEvent(
        comment.getId(),
        comment.getArticleId(),
        comment.getUserId(),
        comment.getDeletedAt()
    ));
    log.info("Comment soft-deleted: commentId={}", commentId);
  }

  @Transactional
  public void hardDeleteComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException());

    int removedLikes = commentLikeRepository.deleteAllByCommentId(commentId);
    commentRepository.delete(comment);

    eventPublisher.publishCommentHardDeleted(new CommentHardDeletedEvent(
        comment.getId(),
        comment.getArticleId(),
        comment.getUserId()
    ));
    log.info("Comment hard-deleted: commentId={}, removedLikes={}", commentId, removedLikes);
  }

  @Transactional
  public CommentLikeResponse likeComment(UUID commentId, UUID requestUserId) {
    // 사전 검증
    userReader.findActiveUser(requestUserId)
        .orElseThrow(() -> new UserNotFoundException());
    Comment comment = findActiveComment(commentId);
    // 중복 체크
    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId)) {
      throw new DuplicateCommentLikeException();
    }

    // 좋아요 이력을 DB에 저장
    CommentLike savedLike = persistCommentLike(comment, requestUserId);

    // 좋아요 증가 (동시성 문제 해결)
    int updatedRows = commentRepository.incrementLikeCount(commentId);
    if (updatedRows == 0) {
      throw new CommentNotFoundException();
    }

    // 카운트 동기화 (DB에서 댓글을 다시 조회하여 실제로 최신 likeCount 가져옴)
    Comment refreshedComment = commentRepository.findByIdAndDeletedFalse(commentId)
        .orElse(comment);

    // 연관 도메인(User, Article) 데이터 조회 - Comment 엔티티만으로는 알 수 없는 외부 도메인 조합
    String articleTitle = articleReader.findActiveArticle(refreshedComment.getArticleId())
        .map(ArticleSummary::title)
        .orElse("");
    String commentUserNickname = resolveNickname(refreshedComment.getUserId());

    // 이벤트 객체 생성(후속 처리를 위한 이벤트)
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

    log.info("Comment liked: commentId={}, userId={}, likeCount={}",
        commentId, requestUserId, refreshedComment.getLikeCount());
    // 매퍼를 통해 CommmentLikeResponse DTO에 매핑하여 반환
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
    findActiveComment(commentId);

    int deletedRows = commentLikeRepository.deleteByCommentIdAndUserId(commentId, requestUserId);
    if (deletedRows == 0) {
      throw new CommentLikeNotFoundException();
    }

    commentRepository.decrementLikeCount(commentId);
    eventPublisher.publishCommentUnliked(new CommentUnlikedEvent(commentId, requestUserId));
    log.info("Comment unliked: commentId={}, userId={}", commentId, requestUserId);
  }

  private Comment findActiveComment(UUID commentId) {
    return commentRepository.findByIdAndDeletedFalse(commentId)
        .orElseThrow(() -> new CommentNotFoundException());
  }

  private void validateAuthor(Comment comment, UUID requestUserId) {
    if (!comment.getUserId().equals(requestUserId)) {
      throw new CommentForbiddenException();
    }
  }

  private CommentLike persistCommentLike(Comment comment, UUID requestUserId) {
    try {
      return commentLikeRepository.saveAndFlush(CommentLike.create(comment, requestUserId));
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateCommentLikeException();
    }
  }

  private List<CommentResponse> mapToResponses(List<Comment> pageComments, UUID requestUserId) {
    if (pageComments.isEmpty()) {
      return List.of();
    }
    Set<UUID> likedCommentIds = resolveLikedCommentIds(pageComments, requestUserId);
    Map<UUID, UserSummary> usersById = resolveUsers(pageComments);

    return pageComments.stream()
        .map(comment -> {
          String nickname = usersById.getOrDefault(
              comment.getUserId(),
              new UserSummary(comment.getUserId(), UNKNOWN_NICKNAME)
          ).nickname();
          return commentMapper.toResponse(comment, nickname, likedCommentIds.contains(comment.getId()));
        })
        .toList();
  }

  private CursorTokens extractCursorTokens(List<Comment> pageComments, CommentOrderBy orderBy, boolean hasNext) {
    if (!hasNext || pageComments.isEmpty()) {
      return new CursorTokens(null, null);
    }
    Comment last = pageComments.get(pageComments.size() - 1);
    String nextCursor = orderBy == CommentOrderBy.LIKE_COUNT
        ? String.valueOf(last.getLikeCount())
        : last.getCreatedAt().toString();
    String nextAfter = last.getCreatedAt().toString();
    return new CursorTokens(nextCursor, nextAfter);
  }

  private Set<UUID> resolveLikedCommentIds(List<Comment> pageComments, UUID requestUserId) {
    if (requestUserId == null || pageComments.isEmpty()) {
      return Set.of();
    }
    List<UUID> commentIds = pageComments.stream().map(Comment::getId).toList();
    return commentLikeRepository.findLikedCommentIds(commentIds, requestUserId);
  }

  private Map<UUID, UserSummary> resolveUsers(List<Comment> pageComments) {
    List<UUID> userIds = pageComments.stream().map(Comment::getUserId).distinct().toList();
    return userReader.findUsersByIds(userIds);
  }

  private String resolveNickname(UUID userId) {
    return userReader.findActiveUser(userId)
        .map(UserSummary::nickname)
        .orElse(UNKNOWN_NICKNAME);
  }

  private record CursorTokens(String nextCursor, String nextAfter) {
  }
}
