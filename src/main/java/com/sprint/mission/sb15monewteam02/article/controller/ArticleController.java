
package com.sprint.mission.sb15monewteam02.article.controller;

import com.sprint.mission.sb15monewteam02.article.dto.ArticleOrder;
import com.sprint.mission.sb15monewteam02.article.dto.ArticlePage;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleResponse;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleSearchRequest;
import com.sprint.mission.sb15monewteam02.article.dto.ArticleViewResponse;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 조회, 검색 및 삭제 API")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

  private final ArticleService articleService;

  public ArticleController(ArticleService articleService) {
    this.articleService = articleService;
  }

  // 1. 기사 목록 조회, 검색, 정렬, 페이지네이션
  @Operation(summary = "기사 목록 조회",
      description = "검색, 관심사, 출처, 날짜 필터링 및 커서 페이지네이션")
  @GetMapping
  public ResponseEntity<ArticlePage> getArticles(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) UUID interestId,
      @RequestParam(required = false) Set<String> sourceIn,
      @RequestParam(required = false) String publishDateFrom,
      @RequestParam(required = false) String publishDateTo,
      @RequestParam(defaultValue = "publishDate") ArticleOrder orderBy,
      @RequestParam(defaultValue = "DESC") Direction direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) String after,
      @RequestParam(defaultValue = "10") int limit,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {

    ArticleSearchRequest request = new ArticleSearchRequest(
        keyword,
        interestId,
        sourceIn == null ? Set.of() : sourceIn,
        parseDate(publishDateFrom),
        parseDate(publishDateTo),
        orderBy,
        direction,
        cursor,
        after,
        limit
    );

    return ResponseEntity.ok(
        articleService.search(request, userId)
    );
  }

  // 2. 기사 상세 조회
  @Operation(summary = "기사 상세 조회")
  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleResponse> getArticle(
      @PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {

    return ResponseEntity.ok(
        articleService.getArticleResponse(articleId, userId)
    );
  }

  // 3. 뉴스 출처 목록 조회
  @Operation(summary = "뉴스 출처 목록 조회")
  @GetMapping("/sources")
  public ResponseEntity<List<String>> getSources() {

    return ResponseEntity.ok(
        articleService.getSources()
    );
  }

  // 4. 기사 조회 기록 등록 및 중복 조회 처리
  @Operation(summary = "기사 조회 기록 등록",
      description = "동일 사용자의 중복 조회는 1회로 처리")
  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<ArticleViewResponse> recordView(
      @PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {

    return ResponseEntity.ok(
        articleService.recordViewResponse(articleId, userId)
    );
  }

  // 5. 기사 논리 삭제
  @Operation(summary = "기사 논리 삭제")
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> softDelete(
      @PathVariable UUID articleId
  ) {

    articleService.softDelete(articleId);

    return ResponseEntity.noContent().build();
  }

  // 6. 기사 물리 삭제
  @Operation(summary = "기사 물리 삭제")
  @DeleteMapping("/{articleId}/hard")
  public ResponseEntity<Void> hardDelete(
      @PathVariable UUID articleId
  ) {

    articleService.hardDelete(articleId);

    return ResponseEntity.noContent().build();
  }


  // 7. 프론트엔드 날짜 형식 변환
  private Instant parseDate(String value) {

    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return Instant.parse(value);
    } catch (DateTimeParseException exception) {
      return LocalDateTime.parse(value)
          .atZone(ZoneId.of("Asia/Seoul"))
          .toInstant();
    }
  }
}
