
package com.sprint.mission.sb15monewteam02.article.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sprint.mission.sb15monewteam02.article.dto.*;
import com.sprint.mission.sb15monewteam02.article.exception.ArticleNotFoundException;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ArticleService articleService;

  // 테스트에서 공통으로 사용할 데이터
  private final UUID articleId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID interestId = UUID.randomUUID();
  private final UUID viewId = UUID.randomUUID();

  private final Instant publishDate =
      Instant.parse("2026-11-25T01:00:00Z");

  private final String userHeader = "Monew-Request-User-ID";

  private ArticleResponse articleResponse() {
    return new ArticleResponse(
        articleId,
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        publishDate,
        "Spring Boot 관련 뉴스",
        3,
        10,
        true
    );
  }

  private ArticlePage articlePage() {
    return new ArticlePage(
        List.of(articleResponse()),
        null,
        null,
        1,
        1,
        false
    );
  }

  private ArticleViewResponse viewResponse() {
    return new ArticleViewResponse(
        viewId,
        userId,
        Instant.parse("2026-11-25T02:00:00Z"),
        articleId,
        "NAVER",
        "https://example.com/news/1",
        "Java 뉴스",
        publishDate,
        "Spring Boot 관련 뉴스",
        3,
        10
    );
  }

  // 1. 기사 목록 조회
  @Test
  void 기사_목록을_정상적으로_조회한다() throws Exception {

    when(articleService.search(any(), eq(userId)))
        .thenReturn(articlePage());

    mockMvc.perform(get("/api/articles")
            .header(userHeader, userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id")
            .value(articleId.toString()))
        .andExpect(jsonPath("$.content[0].title")
            .value("Java 뉴스"))
        .andExpect(jsonPath("$.content[0].viewedByMe")
            .value(true))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  // 2. 검색 조건 및 필터링
  @Test
  void 검색_조건을_서비스에_정상적으로_전달한다() throws Exception {

    ArticleSearchRequest request = new ArticleSearchRequest(
        "Java",
        interestId,
        Set.of("NAVER"),
        Instant.parse("2026-11-24T15:00:00Z"),
        Instant.parse("2026-11-25T14:59:59Z"),
        ArticleOrder.publishDate,
        Direction.DESC,
        null,
        null,
        10
    );

    when(articleService.search(request, userId))
        .thenReturn(articlePage());

    mockMvc.perform(get("/api/articles")
            .header(userHeader, userId.toString())
            .param("keyword", "Java")
            .param("interestId", interestId.toString())
            .param("sourceIn", "NAVER")
            .param("publishDateFrom", "2026-11-25T00:00:00")
            .param("publishDateTo", "2026-11-25T23:59:59")
            .param("orderBy", "publishDate")
            .param("direction", "DESC")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));

    verify(articleService).search(request, userId);
  }

  // 3. 기사 정렬
  @Test
  void 조회수_내림차순_정렬을_요청한다() throws Exception {

    ArticleSearchRequest request = new ArticleSearchRequest(
        null, null, Set.of(),
        null, null,
        ArticleOrder.viewCount,
        Direction.DESC,
        null, null, 10
    );

    when(articleService.search(request, userId))
        .thenReturn(articlePage());

    mockMvc.perform(get("/api/articles")
            .header(userHeader, userId.toString())
            .param("orderBy", "viewCount")
            .param("direction", "DESC"))
        .andExpect(status().isOk());

    verify(articleService).search(request, userId);
  }

  // 4. 커서 기반 페이지네이션
  @Test
  void 커서_기반_페이지네이션을_처리한다() throws Exception {

    String cursor = "2026-11-25T03:00:00Z";
    String after = UUID.randomUUID().toString();

    ArticleSearchRequest request = new ArticleSearchRequest(
        null, null, Set.of(),
        null, null,
        ArticleOrder.publishDate,
        Direction.DESC,
        cursor, after, 1
    );

    ArticlePage page = new ArticlePage(
        List.of(articleResponse()),
        publishDate.toString(),
        articleId.toString(),
        1,
        20,
        true
    );

    when(articleService.search(request, userId))
        .thenReturn(page);

    mockMvc.perform(get("/api/articles")
            .header(userHeader, userId.toString())
            .param("cursor", cursor)
            .param("after", after)
            .param("limit", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(20))
        .andExpect(jsonPath("$.nextCursor")
            .value(publishDate.toString()))
        .andExpect(jsonPath("$.nextAfter")
            .value(articleId.toString()))
        .andExpect(jsonPath("$.hasNext").value(true));

    verify(articleService).search(request, userId);
  }

  // 5. 기사 상세 조회
  @Test
  void 기사_상세_정보를_조회한다() throws Exception {

    when(articleService.getArticleResponse(articleId, userId))
        .thenReturn(articleResponse());

    mockMvc.perform(get("/api/articles/{id}", articleId)
            .header(userHeader, userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
            .value(articleId.toString()))
        .andExpect(jsonPath("$.title").value("Java 뉴스"))
        .andExpect(jsonPath("$.source").value("NAVER"))
        .andExpect(jsonPath("$.commentCount").value(3))
        .andExpect(jsonPath("$.viewCount").value(10))
        .andExpect(jsonPath("$.viewedByMe").value(true));
  }

  // 6. 존재하지 않는 기사 처리
  @Test
  void 존재하지_않는_기사는_404를_반환한다() throws Exception {

    when(articleService.getArticleResponse(articleId, userId))
        .thenThrow(new ArticleNotFoundException(articleId));

    mockMvc.perform(get("/api/articles/{id}", articleId)
            .header(userHeader, userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ART_001"))
        .andExpect(jsonPath("$.message").value("기사를 찾을 수 없습니다."));
  }

  // 7. 뉴스 출처 목록
  @Test
  void 뉴스_출처_목록을_반환한다() throws Exception {

    when(articleService.getSources())
        .thenReturn(List.of(
            "NAVER", "한국경제", "조선일보", "연합뉴스"
        ));

    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0]").value("NAVER"))
        .andExpect(jsonPath("$[1]").value("한국경제"))
        .andExpect(jsonPath("$[2]").value("조선일보"))
        .andExpect(jsonPath("$[3]").value("연합뉴스"));
  }

  // 8. 기사 조회 기록 등록
  @Test
  void 기사_조회_기록을_등록한다() throws Exception {

    when(articleService.recordViewResponse(articleId, userId))
        .thenReturn(viewResponse());

    mockMvc.perform(post("/api/articles/{id}/article-views", articleId)
            .header(userHeader, userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
            .value(viewId.toString()))
        .andExpect(jsonPath("$.viewedBy")
            .value(userId.toString()))
        .andExpect(jsonPath("$.articleId")
            .value(articleId.toString()))
        .andExpect(jsonPath("$.articleTitle")
            .value("Java 뉴스"))
        .andExpect(jsonPath("$.articleViewCount").value(10));
  }

  // 9. 중복 조회 응답 처리
  @Test
  void 동일_사용자의_중복_조회_응답을_처리한다() throws Exception {

    when(articleService.recordViewResponse(articleId, userId))
        .thenReturn(viewResponse());

    for (int i = 0; i < 2; i++) {
      mockMvc.perform(post(
              "/api/articles/{id}/article-views", articleId)
              .header(userHeader, userId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id")
              .value(viewId.toString()))
          .andExpect(jsonPath("$.articleViewCount")
              .value(10));
    }

    verify(articleService, times(2))
        .recordViewResponse(articleId, userId);
  }

  // 10. 기사 논리 삭제
  @Test
  void 기사를_논리_삭제한다() throws Exception {

    mockMvc.perform(delete("/api/articles/{id}", articleId))
        .andExpect(status().isNoContent());

    verify(articleService).softDelete(articleId);
  }

  // 11. 기사 물리 삭제
  @Test
  void 기사를_물리_삭제한다() throws Exception {

    mockMvc.perform(delete("/api/articles/{id}/hard", articleId))
        .andExpect(status().isNoContent());

    verify(articleService).hardDelete(articleId);
  }

  // 12. 기사 목록 조회 시 필수 헤더 누락 검증
  @Test
  void 기사_목록_조회시_사용자_ID가_없으면_400을_반환한다() throws Exception {

    mockMvc.perform(get("/api/articles"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CMN_003"));


    verifyNoInteractions(articleService);
  }

  // 13. 기사 상세 조회 시 필수 헤더 누락 검증
  @Test
  void 기사_상세_조회시_사용자_ID가_없으면_400을_반환한다() throws Exception {

    mockMvc.perform(get("/api/articles/{id}", articleId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CMN_003"));

    verifyNoInteractions(articleService);
  }

  // 14. 잘못된 사용자 UUID 검증
  @Test
  void 사용자_ID_형식이_잘못되면_400을_반환한다() throws Exception {

    mockMvc.perform(get("/api/articles")
            .header(userHeader, "invalid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CMN_002"));

    verifyNoInteractions(articleService);
  }

  // 15. 잘못된 날짜 형식 검증
  @Test
  void 날짜_형식이_잘못되면_400을_반환한다() throws Exception {

    mockMvc.perform(get("/api/articles")
            .header(userHeader, userId.toString())
            .param("publishDateFrom", "invalid-date"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CMN_400"));

    verifyNoInteractions(articleService);
  }
}
