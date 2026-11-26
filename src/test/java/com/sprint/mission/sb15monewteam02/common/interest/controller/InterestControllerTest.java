package com.sprint.mission.sb15monewteam02.common.interest.controller;

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