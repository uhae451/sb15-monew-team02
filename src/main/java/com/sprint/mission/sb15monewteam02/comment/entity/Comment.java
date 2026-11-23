package com.sprint.mission.sb15monewteam02.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comments_article_deleted_created", columnList = "article_id, deleted, created_at"),
        @Index(name = "idx_comments_article_deleted_like_created", columnList = "article_id, deleted, like_count, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "article_id", nullable = false, updatable = false)
  private UUID articleId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "content", nullable = false, length = 500)
  private String content;

  @Column(name = "like_count", nullable = false)
  private long likeCount;

  @Column(name = "deleted", nullable = false)
  private boolean deleted;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  private Comment(UUID articleId, UUID userId, String content, long likeCount, Instant createdAt) {
    this.articleId = articleId;
    this.userId = userId;
    this.content = content;
    this.likeCount = likeCount;
    this.deleted = false;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public static Comment create(UUID articleId, UUID userId, String content) {
    return new Comment(articleId, userId, content, 0L, null);
  }

  public static Comment createWithMetrics(
      UUID articleId,
      UUID userId,
      String content,
      long likeCount,
      Instant createdAt
  ) {
    return new Comment(articleId, userId, content, likeCount, createdAt);
  }

  @PrePersist
  void onPrePersist() {
    Instant now = Instant.now();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    if (this.updatedAt == null) {
      this.updatedAt = this.createdAt;
    }
  }

  @PreUpdate
  void onPreUpdate() {
    this.updatedAt = Instant.now();
  }

  public void updateContent(String content) {
    this.content = content;
  }

  public void softDelete() {
    this.deleted = true;
    this.deletedAt = Instant.now();
  }
}
