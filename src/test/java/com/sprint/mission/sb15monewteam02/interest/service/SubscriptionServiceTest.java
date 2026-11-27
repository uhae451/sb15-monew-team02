package com.sprint.mission.sb15monewteam02.interest.service;

import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.DuplicateSubscriptionException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNotFoundException;
import com.sprint.mission.sb15monewteam02.common.exception.SubscriptionNotFoundException;
import com.sprint.mission.sb15monewteam02.interest.dto.SubscriptionDto;
import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.entity.Subscription;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestRepository;
import com.sprint.mission.sb15monewteam02.interest.repository.SubscriptionRepository;
import com.sprint.mission.sb15monewteam02.user.entity.User;
import com.sprint.mission.sb15monewteam02.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID interestId;
    private UUID userId;
    private Interest interest;
    private User user;

    @BeforeEach
    void setUp() {
        interestId = UUID.randomUUID();
        userId = UUID.randomUUID();
        interest = Interest.create("인공지능", List.of("AI", "머신러닝"));
        user = new User("test@test.com", "tester", "password");
    }

    @Test
    void 관심사를_구독하면_구독을_저장하고_구독자_수를_증가시킨다() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)).thenReturn(false);
        when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SubscriptionDto result = subscriptionService.subscribe(interestId, userId);

        // then
        verify(subscriptionRepository).saveAndFlush(any(Subscription.class));
        verify(interestRepository).incrementSubscriberCount(interestId);
        assertThat(result.interestName()).isEqualTo("인공지능");
        assertThat(result.interestKeywords()).containsExactly("AI", "머신러닝");
    }

    @Test
    void 존재하지_않는_사용자는_구독할_수_없다() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(subscriptionRepository, never()).saveAndFlush(any());
    }

    @Test
    void 탈퇴한_사용자는_구독할_수_없다() {
        // given
        user.delete();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_관심사는_구독할_수_없다() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(interestRepository.findById(interestId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
                .isInstanceOf(InterestNotFoundException.class);
    }

    @Test
    void 이미_구독한_관심사는_다시_구독할_수_없다() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
                .isInstanceOf(DuplicateSubscriptionException.class);

        verify(subscriptionRepository, never()).saveAndFlush(any());
        verify(interestRepository, never()).incrementSubscriberCount(any());
    }

    @Test
    void 동시_요청으로_unique_제약에_걸리면_중복_구독으로_처리한다() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)).thenReturn(false);
        when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
                .isInstanceOf(DuplicateSubscriptionException.class);

        verify(interestRepository, never()).incrementSubscriberCount(any());
    }

    @Test
    void 구독을_취소하면_구독을_삭제하고_구독자_수를_감소시킨다() {
        // given
        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId)).thenReturn(1);

        // when
        subscriptionService.unsubscribe(interestId, userId);

        // then
        verify(interestRepository).decrementSubscriberCount(interestId);
    }

    @Test
    void 구독하지_않은_관심사는_구독_취소할_수_없다() {
        // given
        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> subscriptionService.unsubscribe(interestId, userId))
                .isInstanceOf(SubscriptionNotFoundException.class);

        verify(interestRepository, never()).decrementSubscriberCount(any());
    }

    @Test
    void 존재하지_않는_관심사는_구독_취소할_수_없다() {
        // given
        when(interestRepository.findById(interestId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.unsubscribe(interestId, userId))
                .isInstanceOf(InterestNotFoundException.class);
    }
}
