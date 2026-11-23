package com.sprint.mission.sb15monewteam02.comment.controller;

import com.sprint.mission.sb15monewteam02.comment.dto.CommentLikeResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CommentResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CreateCommentRequest;
import com.sprint.mission.sb15monewteam02.comment.dto.UpdateCommentRequest;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import com.sprint.mission.sb15monewteam02.comment.exception.CommentForbiddenException;
import com.sprint.mission.sb15monewteam02.comment.exception.CommentLikeNotFoundException;
import com.sprint.mission.sb15monewteam02.comment.exception.DuplicateCommentLikeException;
import com.sprint.mission.sb15monewteam02.comment.service.CommentService;
import com.sprint.mission.sb15monewteam02.common.dto.CursorPageResponse;
import com.sprint.mission.sb15monewteam02.common.dto.SortDirection;
import com.sprint.mission.sb15monewteam02.common.logging.MdcLoggingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(MdcLoggingFilter.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("GET /api/comments - 커서 페이지네이션 목록 및 MDC 응답 헤더를 반환한다")
    void getCommentsSuccess() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-23T12:00:00Z");

        CommentResponse item = new CommentResponse(
                commentId, articleId, userId, "테스터", "댓글 내용", 5L, true, now
        );
        CursorPageResponse<CommentResponse> response = CursorPageResponse.of(
                List.of(item), "5", now.toString(), 10L, true
        );

        given(commentService.getComments(any(), eq(userId))).willReturn(response);

        mockMvc.perform(get("/api/comments")
                        .param("articleId", articleId.toString())
                        .param("orderBy", "likeCount")
                        .param("direction", SortDirection.DESC.name())
                        .param("limit", "5")
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().exists(MdcLoggingFilter.HEADER_REQUEST_ID))
                .andExpect(header().exists(MdcLoggingFilter.HEADER_CLIENT_IP))
                .andExpect(jsonPath("$.content[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.content[0].likedByMe").value(true))
                .andExpect(jsonPath("$.nextCursor").value("5"))
                .andExpect(jsonPath("$.nextAfter").value(now.toString()))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("GET /api/comments - limit 범위 초과 또는 잘못된 orderBy면 400을 반환한다")
    void getCommentsValidationFailure() throws Exception {
        UUID articleId = UUID.randomUUID();

        mockMvc.perform(get("/api/comments")
                        .param("articleId", articleId.toString())
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        mockMvc.perform(get("/api/comments")
                        .param("articleId", articleId.toString())
                        .param("orderBy", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TYPE_VALUE"));
    }

    @Test
    @DisplayName("POST /api/comments - 댓글 등록 성공 시 201을 반환한다")
    void createCommentSuccess() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(articleId, userId, "새 댓글");
        CommentResponse response = new CommentResponse(
                commentId, articleId, userId, "테스터", "새 댓글", 0L, false, Instant.now()
        );

        given(commentService.createComment(any(CreateCommentRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value("새 댓글"));
    }

    @Test
    @DisplayName("POST /api/comments - 공백 내용 또는 필수값 누락 시 400을 반환한다")
    void createCommentValidationFailure() throws Exception {
        CreateCommentRequest invalid = new CreateCommentRequest(null, UUID.randomUUID(), "  ");

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/comments/{commentId} - 본인 댓글 수정 성공 시 200, 타인 댓글 수정 시 403을 반환한다")
    void updateCommentSuccessAndForbidden() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UpdateCommentRequest request = new UpdateCommentRequest("수정된 댓글");
        CommentResponse response = new CommentResponse(
                commentId, UUID.randomUUID(), userId, "테스터", "수정된 댓글", 1L, false, Instant.now()
        );

        given(commentService.updateComment(eq(commentId), eq(userId), any(UpdateCommentRequest.class)))
                .willReturn(response);

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 댓글"));

        UUID otherUserId = UUID.randomUUID();
        given(commentService.updateComment(eq(commentId), eq(otherUserId), any(UpdateCommentRequest.class)))
                .willThrow(new CommentForbiddenException());

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Monew-Request-User-ID", otherUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));
    }

    @Test
    @DisplayName("DELETE /api/comments/{commentId} 및 /hard - 논리·물리 삭제 시 204를 반환한다")
    void deleteCommentSoftAndHard() throws Exception {
        UUID commentId = UUID.randomUUID();
        willDoNothing().given(commentService).softDeleteComment(commentId, null);
        willDoNothing().given(commentService).hardDeleteComment(commentId);

        mockMvc.perform(delete("/api/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/comments/{commentId}/comment-likes - 좋아요 성공 시 201, 중복 시 409를 반환한다")
    void likeCommentSuccessAndDuplicate() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CommentLikeResponse response = new CommentLikeResponse(
                UUID.randomUUID(),
                userId,
                Instant.now(),
                commentId,
                UUID.randomUUID(),
                "기사 제목",
                UUID.randomUUID(),
                "작성자",
                "댓글 내용",
                1L,
                Instant.now()
        );

        given(commentService.likeComment(commentId, userId)).willReturn(response);

        mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(commentId.toString()))
                .andExpect(jsonPath("$.commentLikeCount").value(1));

        given(commentService.likeComment(commentId, userId)).willThrow(new DuplicateCommentLikeException());

        mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_COMMENT_LIKE"));
    }

    @Test
    @DisplayName("DELETE /api/comments/{commentId}/comment-likes - 취소 성공 시 204, 내역 없으면 404를 반환한다")
    void unlikeCommentSuccessAndNotFound() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        willDoNothing().given(commentService).unlikeComment(commentId, userId);

        mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isNoContent());

        willThrow(new CommentLikeNotFoundException()).given(commentService).unlikeComment(commentId, userId);

        mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_LIKE_NOT_FOUND"));
    }
}
