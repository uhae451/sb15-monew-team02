package com.sprint.mission.sb15monewteam02.interest.dto;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;

import java.util.List;
import java.util.UUID;

public record InterestDto(
        UUID id,
        String name,
        List<String> keywords,
        long subscriberCount,
        // 요청한 사용자(Monew-Request-User-ID)가 이 관심사를 구독 중인지 여부
        boolean subscribedByMe
) {

    // 구독 여부를 알 필요 없는 응답용 (등록, 키워드 수정)
    public static InterestDto from(Interest interest) {
        return from(interest, false);
    }

    public static InterestDto from(Interest interest, boolean subscribedByMe) {
        return new InterestDto(
                interest.getId(),
                interest.getName(),
                interest.getKeywords(),
                interest.getSubscriberCount(),
                subscribedByMe
        );
    }
}
