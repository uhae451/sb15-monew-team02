package com.sprint.mission.sb15monewteam02.article.repository;

import com.sprint.mission.sb15monewteam02.article.entity.Article;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleSearchRepository {
  boolean existsBySourceUrl(String sourceUrl);

  Optional<Article> findByIdAndDeletedFalse(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Article a where a.id = :id")
  Optional<Article> findForUpdate(@Param("id") UUID id);
}
