package com.sprint.mission.sb15monewteam02.interest.dto;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.entity.Subscription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// 구독 응답 (프론트 활동 내역에서 interestId, interestName, interestKeywords, interestSubscriberCount를 읽는다)
public record SubscriptionDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt
) {

    public static SubscriptionDto of(Subscription subscription, Interest interest) {
        return new SubscriptionDto(
                subscription.getId(),
                interest.getId(),
                interest.getName(),
                interest.getKeywords(),
                interest.getSubscriberCount(),
                subscription.getCreatedAt()
        );
    }
}
