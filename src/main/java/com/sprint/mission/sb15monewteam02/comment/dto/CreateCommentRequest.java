package com.sprint.mission.sb15monewteam02.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommentRequest(
        @NotNull(message = "기사 ID는 필수입니다.")
        UUID articleId,

        @NotNull(message = "사용자 ID는 필수입니다.")
        UUID userId,

        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 500, message = "댓글 내용은 500자를 초과할 수 없습니다.")
        String content
) {
}
