package com.sprint.mission.sb15monewteam02.article.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleCreateRequest(
    @NotBlank String source, @NotBlank String sourceUrl, @NotBlank String title,
    @NotNull Instant publishDate, @NotBlank String summary) {}
