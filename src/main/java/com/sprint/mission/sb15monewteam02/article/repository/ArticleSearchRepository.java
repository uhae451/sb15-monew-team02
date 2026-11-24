package com.sprint.mission.sb15monewteam02.article.repository;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleSearchRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSlice;

public interface ArticleSearchRepository {
  ArticleSlice search(ArticleSearchRequest request);
}
