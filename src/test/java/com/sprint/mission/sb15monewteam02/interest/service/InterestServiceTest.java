package com.sprint.mission.sb15monewteam02.interest.service;

import com.sprint.mission.sb15monewteam02.common.exception.InterestKeywordEmptyException;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNameSimilarException;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNotFoundException;
import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestRepository;
import com.sprint.mission.sb15monewteam02.interest.repository.SubscriptionRepository;
import org.mockito.InOrder;
import com.sprint.mission.sb15monewteam02.interest.service.InterestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private InterestService interestService;

    @BeforeEach
    void setUp() {
        interestService = new InterestService(interestRepository, subscriptionRepository);
    }

    @Test
    void 유사한_이름의_관심사가_있으면_등록할_수_없다() {

        // given
        Interest existing = Interest.create("주식시장들", List.of("주식"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));

        // when & then
        assertThatThrownBy(() -> interestService.register("주식시장은", List.of("증시")))
                .isInstanceOf(InterestNameSimilarException.class);

        verify(interestRepository, never()).save(any(Interest.class));
    }

    @Test
    void 유사한_이름이_없으면_관심사를_저장한다() {

        // given
        Interest existing = Interest.create("경제", List.of("금융"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // when
        Interest result = interestService.register("인공지능", List.of("AI"));

        // then
        assertThat(result.getName()).isEqualTo("인공지능");
        verify(interestRepository).save(any(Interest.class));
    }

    @Test
    void 대소문자만_다른_이름은_유사한_이름으로_보고_등록할_수_없다() {

        // given
        Interest existing = Interest.create("Economy", List.of("경제"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));

        // when & then
        assertThatThrownBy(() -> interestService.register("ECONOMY", List.of("경제")))
                .isInstanceOf(InterestNameSimilarException.class);

        verify(interestRepository, never()).save(any(Interest.class));
    }

    @Test
    void 앞뒤_공백만_다른_이름은_유사한_이름으로_보고_등록할_수_없다() {

        // given
        Interest existing = Interest.create("주식", List.of("증시"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));

        // when & then
        assertThatThrownBy(() -> interestService.register("  주식  ", List.of("증시")))
                .isInstanceOf(InterestNameSimilarException.class);

        verify(interestRepository, never()).save(any(Interest.class));
    }

    @Test
    void 관심사를_생성하면_여러_키워드가_그대로_보존된다() {

        // when
        Interest interest = Interest.create("인공지능", List.of("AI", "머신러닝"));
        //then
        assertThat(interest.getKeywords()).containsExactly("AI", "머신러닝");
    }

    @Test
    void 관심사_이름의_앞뒤_공백은_제거하고_저장한다() {

        // given
        when(interestRepository.findAll()).thenReturn(List.of());
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Interest result = interestService.register("  인공지능  ", List.of("AI"));

        // then
        assertThat(result.getName()).isEqualTo("인공지능");
    }

    @Test
    void 등록된_관심사가_하나도_없으면_바로_저장한다() {

        // given
        when(interestRepository.findAll()).thenReturn(List.of());
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // when
        Interest result = interestService.register("인공지능", List.of("AI"));

        // then
        assertThat(result.getName()).isEqualTo("인공지능");
        verify(interestRepository).save(any(Interest.class));
    }

    @Test
    void 유사도가_80퍼센트_미만이면_등록할_수_있다() {

        // given
        Interest existing = Interest.create("인공지능", List.of("AI"));
        when(interestRepository.findAll()).thenReturn(List.of(existing));
        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when & then
        // "인공지능"과 "인공지능학습": 편집 거리 2, 더 긴 길이 6 → 유사도 약 0.67 (< 0.8)
        // 레벤슈타인 검증
        assertThatCode(() -> interestService.register("인공지능학습", List.of("AI")))
                .doesNotThrowAnyException();

        verify(interestRepository).save(any(Interest.class));
    }

    @Test
    void 관심사를_삭제한다() {

        // given
        UUID id = UUID.randomUUID();
        Interest interest = Interest.create("인공지능", List.of("AI"));
        when(interestRepository.findById(id)).thenReturn(Optional.of(interest));

        // when
        interestService.delete(id);

        // then: 구독 내역을 먼저 지운 뒤 관심사를 삭제한다
        InOrder inOrder = inOrder(subscriptionRepository, interestRepository);
        inOrder.verify(subscriptionRepository).deleteAllByInterestId(id);
        inOrder.verify(interestRepository).delete(interest);
    }

    @Test
    void 존재하지_않는_관심사는_삭제할_수_없다() {

        // given
        UUID id = UUID.randomUUID();
        when(interestRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> interestService.delete(id))
                .isInstanceOf(InterestNotFoundException.class);

        verify(interestRepository, never()).delete(any(Interest.class));
    }


    @Test
    void 관심사_키워드를_수정한다() {

        // given
        UUID id = UUID.randomUUID();
        Interest interest = Interest.create("인공지능", List.of("AI"));
        when(interestRepository.findById(id)).thenReturn(Optional.of(interest));

        // when
        interestService.updateKeywords(id, List.of("LLM", "딥러닝"));

        // then
        assertThat(interest.getKeywords()).containsExactly("LLM", "딥러닝");
    }

    @Test
    void 존재하지_않는_관심사는_수정할_수_없다() {

        // given
        UUID id = UUID.randomUUID();
        when(interestRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> interestService.updateKeywords(id, List.of("LLM")))
                .isInstanceOf(InterestNotFoundException.class);
    }

    @Test
    void 빈_키워드로_수정하면_기존_키워드가_유지된다() {

        // given
        UUID id = UUID.randomUUID();
        Interest interest = Interest.create("인공지능", List.of("AI"));
        when(interestRepository.findById(id)).thenReturn(Optional.of(interest));

        // when & then
        assertThatThrownBy(() -> interestService.updateKeywords(id, List.of()))
                .isInstanceOf(InterestKeywordEmptyException.class);

        assertThat(interest.getKeywords()).containsExactly("AI");
    }
}