package com.sprint.mission.sb15monewteam02.interest.controller;

import com.sprint.mission.sb15monewteam02.common.exception.DuplicateSubscriptionException;
import com.sprint.mission.sb15monewteam02.common.exception.InterestKeywordEmptyException;
import com.sprint.mission.sb15monewteam02.common.exception.SubscriptionNotFoundException;
import com.sprint.mission.sb15monewteam02.interest.dto.GetInterestResponse;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestDto;
import com.sprint.mission.sb15monewteam02.interest.dto.SubscriptionDto;
import com.sprint.mission.sb15monewteam02.interest.service.SubscriptionService;
import java.time.Instant;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNameSimilarException;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNotFoundException;
import com.sprint.mission.sb15monewteam02.interest.controller.InterestController;
import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.service.InterestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterestService interestService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private static final String USER_HEADER = "Monew-Request-User-ID";

    @Test
    void 관심사_등록_요청이_성공하면_201을_응답한다() throws Exception {
        Interest interest = Interest.create("인공지능", List.of("AI"));
        when(interestService.register("인공지능", List.of("AI"))).thenReturn(interest);

        mockMvc.perform(post("/api/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "인공지능", "keywords": ["AI"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("인공지능"));
    }

    @Test
    void 유사한_이름이면_400을_응답한다() throws Exception {
        when(interestService.register(any(), any()))
                .thenThrow(new InterestNameSimilarException());

        mockMvc.perform(post("/api/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "주식시장은", "keywords": ["증시"]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 키워드_수정_요청이_성공하면_200을_응답한다() throws Exception {
        UUID id = UUID.randomUUID();
        Interest interest = Interest.create("인공지능", List.of("LLM"));
        when(interestService.updateKeywords(eq(id), eq(List.of("LLM")))).thenReturn(interest);

        mockMvc.perform(patch("/api/interests/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keywords": ["LLM"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("인공지능"));
    }

    @Test
    void 존재하지_않는_관심사의_키워드를_수정하면_404를_응답한다() throws Exception {
        UUID id = UUID.randomUUID();
        when(interestService.updateKeywords(eq(id), any()))
                .thenThrow(new InterestNotFoundException());

        mockMvc.perform(patch("/api/interests/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keywords": ["LLM"]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITR_004"));
    }

    @Test
    void 빈_키워드로_수정하면_400을_응답한다() throws Exception {
        UUID id = UUID.randomUUID();
        when(interestService.updateKeywords(eq(id), eq(List.of())))
                .thenThrow(new InterestKeywordEmptyException());

        mockMvc.perform(patch("/api/interests/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keywords": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITR_001"));
    }

    @Test
    void 관심사_조회_시_사용자_헤더를_서비스로_전달하고_subscribedByMe를_응답한다() throws Exception {
        UUID userId = UUID.randomUUID();
        InterestDto dto = new InterestDto(UUID.randomUUID(), "인공지능", List.of("AI"), 1L, true);
        when(interestService.search(any(), eq(userId)))
                .thenReturn(new GetInterestResponse(List.of(dto), null, null, 1, 1L, false));

        mockMvc.perform(get("/api/interests").header(USER_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].subscribedByMe").value(true));

        verify(interestService).search(any(), eq(userId));
    }

    @Test
    void 사용자_헤더_없이_조회해도_200을_응답한다() throws Exception {
        when(interestService.search(any(), isNull()))
                .thenReturn(new GetInterestResponse(List.of(), null, null, 0, 0L, false));

        mockMvc.perform(get("/api/interests"))
                .andExpect(status().isOk());

        verify(interestService).search(any(), isNull());
    }

    @Test
    void 구독_요청이_성공하면_200과_구독_정보를_응답한다() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SubscriptionDto dto = new SubscriptionDto(
                UUID.randomUUID(), interestId, "인공지능", List.of("AI"), 1L, Instant.now());
        when(subscriptionService.subscribe(interestId, userId)).thenReturn(dto);

        mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
                        .header(USER_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestId").value(interestId.toString()))
                .andExpect(jsonPath("$.interestName").value("인공지능"))
                .andExpect(jsonPath("$.interestSubscriberCount").value(1));
    }

    @Test
    void 사용자_헤더_없이_구독하면_400을_응답한다() throws Exception {
        mockMvc.perform(post("/api/interests/{id}/subscriptions", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CMN_003"));
    }

    @Test
    void 이미_구독한_관심사를_구독하면_409를_응답한다() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(subscriptionService.subscribe(interestId, userId))
                .thenThrow(new DuplicateSubscriptionException());

        mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
                        .header(USER_HEADER, userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ITR_005"));
    }

    @Test
    void 구독_취소_요청이_성공하면_200을_응답한다() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/interests/{id}/subscriptions", interestId)
                        .header(USER_HEADER, userId))
                .andExpect(status().isOk());

        verify(subscriptionService).unsubscribe(interestId, userId);
    }

    @Test
    void 구독하지_않은_관심사를_구독_취소하면_404를_응답한다() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new SubscriptionNotFoundException())
                .when(subscriptionService).unsubscribe(interestId, userId);

        mockMvc.perform(delete("/api/interests/{id}/subscriptions", interestId)
                        .header(USER_HEADER, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITR_006"));
    }

    @Test
    void 관심사_삭제_요청이_성공하면_204를_응답한다() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/interests/{id}", id))
                .andExpect(status().isNoContent());

        verify(interestService).delete(id);
    }

    @Test
    void 존재하지_않는_관심사를_삭제하면_404를_응답한다() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new InterestNotFoundException()).when(interestService).delete(id);

        mockMvc.perform(delete("/api/interests/{id}", id))
                .andExpect(status().isNotFound());
    }
}