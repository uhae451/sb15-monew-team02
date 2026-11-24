package com.sprint.mission.sb15monewteam02.common.interest.service;

import com.sprint.mission.sb15monewteam02.common.exception.InterestNameSimilarException;
import com.sprint.mission.sb15monewteam02.common.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.common.interest.repository.InterestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;

    private InterestService interestService;

    @BeforeEach
    void setUp() {
        interestService = new InterestService(interestRepository);
    }

    @Test
    void 유사한_이름의_관심사가_있으면_등록할_수_없다() {
        Interest existing = Interest.create("주식시장들", List.of("주식"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> interestService.register("주식시장은", List.of("증시")))
                .isInstanceOf(InterestNameSimilarException.class);

        verify(interestRepository, never()).save(any(Interest.class));
    }

    @Test
    void 유사한_이름이_없으면_관심사를_저장한다() {
        Interest existing = Interest.create("경제", List.of("금융"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Interest result = interestService.register("인공지능", List.of("AI"));

        assertThat(result.getName()).isEqualTo("인공지능");
        verify(interestRepository).save(any(Interest.class));
    }

    @Test
    void 관심사를_생성하면_여러_키워드가_그대로_보존된다() {
        Interest interest = Interest.create("인공지능", List.of("AI", "머신러닝"));

        assertThat(interest.getKeywords()).containsExactly("AI", "머신러닝");
    }

    @Test
    void 등록된_관심사가_하나도_없으면_바로_저장한다() {
        when(interestRepository.findAll()).thenReturn(List.of());
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Interest result = interestService.register("인공지능", List.of("AI"));

        assertThat(result.getName()).isEqualTo("인공지능");
        verify(interestRepository).save(any(Interest.class));
    }

    @Test
    void 유사도가_80퍼센트_미만이면_등록할_수_있다() {
        Interest existing = Interest.create("인공지능", List.of("AI"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // "인공지능"과 "인공지능학습": 편집 거리 2, 더 긴 길이 6 → 유사도 약 0.67 (< 0.8)
        assertThatCode(() -> interestService.register("인공지능학습", List.of("AI")))
                .doesNotThrowAnyException();

        verify(interestRepository).save(any(Interest.class));
    }
}