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
import com.sprint.mission.sb15monewteam02.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final InterestRepository interestRepository;
    private final UserRepository userRepository;

    // 관심사 구독
    public SubscriptionDto subscribe(UUID interestId, UUID userId) {
        validateActiveUser(userId);
        Interest interest = findInterest(interestId);

        // 이미 구독했는지 먼저 확인
        if (subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)) {
            throw new DuplicateSubscriptionException();
        }
        Subscription saved = persistSubscription(interest, userId);

        // 구독자 수 +1 (DB에서 직접 증가)
        interestRepository.incrementSubscriberCount(interestId);

        // 증가된 구독자 수를 응답에 담기 위해 다시 조회
        Interest refreshed = findInterest(interestId);
        return SubscriptionDto.of(saved, refreshed);
    }

    // 관심사 구독 취소
    public void unsubscribe(UUID interestId, UUID userId) {
        findInterest(interestId);

        int deletedRows = subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId);
        if (deletedRows == 0) {
            throw new SubscriptionNotFoundException();
        }

        // 구독자 수 -1 (DB에서 직접 감소)
        interestRepository.decrementSubscriberCount(interestId);
    }

    private void validateActiveUser(UUID userId) {
        userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Interest findInterest(UUID interestId) {
        return interestRepository.findById(interestId)
                .orElseThrow(InterestNotFoundException::new);
    }

    // 동시에 같은 구독 요청이 들어와 exists 검사를 둘 다 통과해도, DB unique 제약에서 막히면 중복 구독으로 처리
    private Subscription persistSubscription(Interest interest, UUID userId) {
        try {
            return subscriptionRepository.saveAndFlush(Subscription.create(interest, userId));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateSubscriptionException();
        }
    }
}
