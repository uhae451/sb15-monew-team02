package com.sprint.mission.sb15monewteam02.article.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "articles", uniqueConstraints =
    @UniqueConstraint(name = "uk_articles_source_url", columnNames = "source_url"))
@Getter
public class Article {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, columnDefinition = "text")
  private String source;

  @Column(name = "source_url", nullable = false, columnDefinition = "text")
  private String sourceUrl;

  @Column(nullable = false, columnDefinition = "text")
  private String title;

  @Column(nullable = false)
  private Instant publishDate;

  @Column(nullable = false, columnDefinition = "text")
  private String summary;

  @Column(nullable = false)
  private long viewCount;

  @Column(nullable = false)
  private long commentCount;

  @Column(nullable = false)
  private boolean deleted;

  // Temporary ID-only association until the interest domain's mapping is agreed.
  @ElementCollection
  @CollectionTable(name = "article_interests", joinColumns = @JoinColumn(name = "article_id"),
      uniqueConstraints = @UniqueConstraint(name = "uk_article_interest",
          columnNames = {"article_id", "interest_id"}))
  @Column(name = "interest_id", nullable = false)
  private Set<UUID> interestIds = new HashSet<>();

  protected Article() {}

  public Article(String source, String sourceUrl, String title, Instant publishDate, String summary) {
    requireText(source, "source");
    requireText(sourceUrl, "sourceUrl");
    requireText(title, "title");
    requireText(summary, "summary");
    if (publishDate == null) throw new IllegalArgumentException("publishDate is required");
    URI uri = URI.create(sourceUrl);
    if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
        || uri.getHost() == null || uri.getUserInfo() != null) {
      throw new IllegalArgumentException("sourceUrl must be an absolute HTTP(S) URL without credentials");
    }
    this.source = source;
    this.sourceUrl = sourceUrl;
    this.title = title;
    this.publishDate = publishDate.truncatedTo(ChronoUnit.MICROS);
    this.summary = summary;
  }

  public void softDelete() {
    deleted = true;
  }

  public void recordView() {
    viewCount = Math.addExact(viewCount, 1);
  }

  public void replaceInterests(Set<UUID> ids) {
    if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("interest IDs are required");
    }
    interestIds.clear();
    interestIds.addAll(ids);
  }

  public Set<UUID> getInterestIds() {
    return Set.copyOf(interestIds);
  }

  public void updateCommentCount(long count) {
    if (count < 0) throw new IllegalArgumentException("commentCount must be nonnegative");
    commentCount = count;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
  }
}
