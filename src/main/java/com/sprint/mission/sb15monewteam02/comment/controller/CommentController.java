package com.sprint.mission.sb15monewteam02.comment.controller;

import com.sprint.mission.sb15monewteam02.comment.dto.CommentLikeResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CommentResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CommentSearchCondition;
import com.sprint.mission.sb15monewteam02.comment.dto.CreateCommentRequest;
import com.sprint.mission.sb15monewteam02.comment.dto.UpdateCommentRequest;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.comment.service.CommentService;
import com.sprint.mission.sb15monewteam02.common.dto.CursorPageResponse;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Comment", description = "댓글 및 좋아요 API")
@Validated
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private static final String REQUEST_USER_HEADER = "Monew-Request-User-ID";

  private final CommentService commentService;

  @Operation(summary = "댓글 목록 조회 (커서 페이지네이션, 정렬 기준 1개)")
  @GetMapping
  public ResponseEntity<CursorPageResponse<CommentResponse>> getComments(
      @RequestParam(required = false) UUID articleId,
      @RequestParam(defaultValue = "createdAt") CommentOrderBy orderBy,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) String after,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
      @RequestHeader(value = REQUEST_USER_HEADER) UUID requestUserId
  ) {
    CommentSearchCondition condition = new CommentSearchCondition(
        articleId, orderBy, direction, cursor, after, limit
    );
    return ResponseEntity.ok(commentService.getComments(condition, requestUserId));
  }

  @Operation(summary = "댓글 등록")
  @PostMapping
  public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CreateCommentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(request));
  }

  @Operation(summary = "댓글 수정 (본인 댓글만 가능)")
  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentResponse> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader(REQUEST_USER_HEADER) UUID requestUserId,
      @Valid @RequestBody UpdateCommentRequest request
  ) {
    return ResponseEntity.ok(commentService.updateComment(commentId, requestUserId, request));
  }

  @Operation(summary = "댓글 논리 삭제")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> softDeleteComment(@PathVariable UUID commentId) {
    commentService.softDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "댓글 물리 삭제 (연관 좋아요 포함)")
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(@PathVariable UUID commentId) {
    commentService.hardDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "댓글 좋아요")
  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeResponse> likeComment(
      @PathVariable UUID commentId,
      @RequestHeader(REQUEST_USER_HEADER) UUID requestUserId
  ) {
    return ResponseEntity.status(HttpStatus.OK).body(commentService.likeComment(commentId, requestUserId));
  }

  @Operation(summary = "댓글 좋아요 취소")
  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> unlikeComment(
      @PathVariable UUID commentId,
      @RequestHeader(REQUEST_USER_HEADER) UUID requestUserId
  ) {
    commentService.unlikeComment(commentId, requestUserId);
    return ResponseEntity.ok().build();
  }
}
