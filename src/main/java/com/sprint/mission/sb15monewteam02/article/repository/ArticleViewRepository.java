package com.sprint.mission.sb15monewteam02.article.repository;

import com.sprint.mission.sb15monewteam02.article.entity.ArticleView;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {
  boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);
  long countByArticleId(UUID articleId);
  @Query("select v.article.id from ArticleView v where v.userId = :userId and v.article.id in :articleIds")
  java.util.Set<UUID> findViewedArticleIds(@Param("userId") UUID userId, @Param("articleIds") java.util.Collection<UUID> articleIds);
  @Modifying
  @Query("delete from ArticleView v where v.article.id = :articleId")
  void deleteAllByArticleId(@Param("articleId") UUID articleId);
}
