package com.sprint.mission.sb15monewteam02.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 500, message = "댓글 내용은 500자를 초과할 수 없습니다.")
        String content
) {
}
