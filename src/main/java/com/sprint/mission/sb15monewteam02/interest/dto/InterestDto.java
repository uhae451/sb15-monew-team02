package com.sprint.mission.sb15monewteam02.interest.dto;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;

import java.util.List;
import java.util.UUID;

public record InterestDto(UUID id, String name, List<String> keywords, long subscriberCount) {

    public static InterestDto from(Interest interest){
        return new InterestDto(
                interest.getId(),
                interest.getName(),
                interest.getKeywords(),
                interest.getSubscriberCount()
        );
    }
}
