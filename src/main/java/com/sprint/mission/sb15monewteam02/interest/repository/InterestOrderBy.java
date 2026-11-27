package com.sprint.mission.sb15monewteam02.interest.repository;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum InterestOrderBy {
    NAME("name"),
    SUBSCRIBER_COUNT("subscriberCount");

    // 프론트가 보내는 값 (name, subscriberCount)
    @JsonValue
    private final String value;

    // 프론트 값(name, subscriberCount)과 enum 이름(NAME, SUBSCRIBER_COUNT) 모두 허용
    @JsonCreator
    public static InterestOrderBy from(String value) {
        if (value == null || value.isBlank()) {
            return NAME;
        }
        return Arrays.stream(values())
                .filter(orderBy -> orderBy.value.equalsIgnoreCase(value) || orderBy.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TYPE_VALUE));
    }
}
