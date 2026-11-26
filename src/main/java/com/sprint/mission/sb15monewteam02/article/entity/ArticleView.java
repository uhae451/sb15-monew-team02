package com.sprint.mission.sb15monewteam02.article.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "article_views", uniqueConstraints =
    @UniqueConstraint(name = "uk_article_views_article_user", columnNames = {"article_id", "user_id"}))
@Getter
public class ArticleView {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;
  @Column(name = "user_id", nullable = false)
  private UUID userId;
  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private Instant createdAt;
  protected ArticleView() {}
  public ArticleView(Article article, UUID userId) {
    if (article == null || userId == null) throw new IllegalArgumentException("article and userId are required");
    this.article = article;
    this.userId = userId;
    this.createdAt = Instant.now();
  }
}
