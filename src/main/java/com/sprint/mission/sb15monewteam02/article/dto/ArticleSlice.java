package com.sprint.mission.sb15monewteam02.article.dto;

import com.sprint.mission.sb15monewteam02.article.entity.Article;
import java.util.List;

public record ArticleSlice(List<Article> content, boolean hasNext, long totalElements) {}
