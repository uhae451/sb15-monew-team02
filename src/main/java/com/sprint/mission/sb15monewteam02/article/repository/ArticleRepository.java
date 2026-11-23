package com.sprint.mission.sb15monewteam02.article.repository;

import com.sprint.mission.sb15monewteam02.article.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

  boolean existsBySourceUrl(String sourceUrl);
}