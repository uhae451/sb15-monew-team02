package com.sprint.mission.sb15monewteam02.article.repository;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleSearchRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSlice;
import com.sprint.mission.sb15monewteam02.article.entity.Article;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class ArticleSearchRepositoryImpl implements ArticleSearchRepository {
  private final EntityManager entityManager;

  public ArticleSearchRepositoryImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public ArticleSlice search(ArticleSearchRequest request) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Article> query = cb.createQuery(Article.class);
    Root<Article> root = query.from(Article.class);
    List<Predicate> conditions = filters(cb, root, request);
    if (request.cursor() != null) conditions.add(cursorPredicate(cb, root, request));
    Path<?> order = root.get(request.orderBy().name());
    boolean asc = request.direction() == Direction.ASC;
    query.where(conditions.toArray(Predicate[]::new));
    query.orderBy(asc ? cb.asc(order) : cb.desc(order), asc ? cb.asc(root.get("id")) : cb.desc(root.get("id")));
    List<Article> rows = entityManager.createQuery(query).setMaxResults(request.limit() + 1).getResultList();
    boolean hasNext = rows.size() > request.limit();

    CriteriaQuery<Long> count = cb.createQuery(Long.class);
    Root<Article> countRoot = count.from(Article.class);
    count.select(cb.count(countRoot)).where(filters(cb, countRoot, request).toArray(Predicate[]::new));
    long total = entityManager.createQuery(count).getSingleResult();
    return new ArticleSlice(List.copyOf(rows.subList(0, Math.min(rows.size(), request.limit()))), hasNext, total);
  }

  private List<Predicate> filters(CriteriaBuilder cb, Root<Article> root, ArticleSearchRequest request) {
    List<Predicate> result = new ArrayList<>();
    result.add(cb.isFalse(root.get("deleted")));
    if (request.keyword() != null && !request.keyword().isBlank()) {
      String pattern = "%" + request.keyword().toLowerCase(Locale.ROOT)
          .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
      result.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
          cb.like(cb.lower(root.get("summary")), pattern, '\\')));
    }
    if (request.interestId() != null) result.add(cb.isMember(request.interestId(), root.get("interestIds")));
    if (request.sourceIn() != null && !request.sourceIn().isEmpty()) result.add(root.get("source").in(request.sourceIn()));
    if (request.publishDateFrom() != null) result.add(cb.greaterThanOrEqualTo(root.get("publishDate"), request.publishDateFrom()));
    if (request.publishDateTo() != null) result.add(cb.lessThanOrEqualTo(root.get("publishDate"), request.publishDateTo()));
    return result;
  }

  private Predicate cursorPredicate(CriteriaBuilder cb, Root<Article> root, ArticleSearchRequest request) {
    boolean asc = request.direction() == Direction.ASC;
    Predicate beyond;
    Predicate equal;
    if (request.orderBy() == com.sprint.mission.sb15monewteam02.article.dto.ArticleOrder.publishDate) {
      Instant value = Instant.parse(request.cursor());
      Path<Instant> path = root.get("publishDate");
      beyond = asc ? cb.greaterThan(path, value) : cb.lessThan(path, value);
      equal = cb.equal(path, value);
    } else {
      long value = Long.parseLong(request.cursor());
      Path<Long> path = root.get(request.orderBy().name());
      beyond = asc ? cb.gt(path, value) : cb.lt(path, value);
      equal = cb.equal(path, value);
    }
    Path<UUID> id = root.get("id");
    UUID after = UUID.fromString(request.after());
    Predicate tie = asc ? cb.greaterThan(id, after) : cb.lessThan(id, after);
    return cb.or(beyond, cb.and(equal, tie));
  }
}
