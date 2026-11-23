package com.sprint.mission.sb15monewteam02.common;

import com.sprint.mission.sb15monewteam02.common.dto.CursorPageResponse;
import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;
import com.sprint.mission.sb15monewteam02.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommonResponseTest {

  private MockMvc mockMvc;
  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = spy(new GlobalExceptionHandler());
    mockMvc = MockMvcBuilders
        .standaloneSetup(new TestController())
        .setControllerAdvice(exceptionHandler)
        .build();
  }

  @Test
  void 목록_응답은_프론트가_기대하는_구조로_반환한다() throws Exception {
    mockMvc.perform(get("/test/page"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value("sample-id"))
        .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
        .andExpect(jsonPath("$.nextAfter").value("2026-11-23T00:00:00Z"))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void 빈_목록에서도_배열과_null_커서_필드를_유지한다() throws Exception {
    mockMvc.perform(get("/test/empty-page"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.nextCursor").hasJsonPath())
        .andExpect(jsonPath("$.nextCursor").value(nullValue()))
        .andExpect(jsonPath("$.nextAfter").hasJsonPath())
        .andExpect(jsonPath("$.nextAfter").value(nullValue()))
        .andExpect(jsonPath("$.size").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  void 업무_예외는_지정한_상태와_오류_본문을_반환한다() throws Exception {
    // 현재 정의된 코드로 검증한다. 업무 코드 추가 후 4xx 매핑도 보강할 수 있다.
    ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;

    mockMvc.perform(get("/test/business-error"))
        .andExpect(status().is(errorCode.getHttpStatus().value()))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value(errorCode.getCode()))
        .andExpect(jsonPath("$.message").value(errorCode.getMessage()));

    // 일반 예외 처리도 같은 500을 반환하므로 업무 예외 전용 메서드의 실행까지 확인한다.
    verify(exceptionHandler).handleBusinessException(any(BusinessException.class));
  }

  @Test
  void 일반_예외는_500을_반환하고_내부_정보를_노출하지_않는다() throws Exception {
    mockMvc.perform(get("/test/unexpected-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$.code").value("CMN_999"))
        .andExpect(jsonPath("$.message").value("서버에 문제가 발생했습니다"))
        .andExpect(content().string(not(containsString("secret-db-info"))))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  // 실제 서비스에 포함되지 않는 테스트 전용 컨트롤러.
  @RestController
  static class TestController {

    @GetMapping("/test/page")
    public ResponseEntity<CursorPageResponse<SampleResponse>> page() {
      return ResponseEntity.ok(new CursorPageResponse<>(
          List.of(new SampleResponse("sample-id")),
          "next-cursor", "2026-11-23T00:00:00Z", 1, 2, true));
    }

    @GetMapping("/test/empty-page")
    public ResponseEntity<CursorPageResponse<SampleResponse>> emptyPage() {
      return ResponseEntity.ok(new CursorPageResponse<>(
          List.of(), null, null, 0, 0, false));
    }

    @GetMapping("/test/business-error")
    public void businessError() {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }

    @GetMapping("/test/unexpected-error")
    public void unexpectedError() {
      throw new RuntimeException("secret-db-info");
    }
  }

  record SampleResponse(String id) {
  }
}
