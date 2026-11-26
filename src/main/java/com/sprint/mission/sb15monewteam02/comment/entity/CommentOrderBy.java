package com.sprint.mission.sb15monewteam02.comment.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sprint.mission.sb15monewteam02.common.exception.BusinessException;
import com.sprint.mission.sb15monewteam02.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CommentOrderBy {
  CREATED_AT("createdAt"),
  LIKE_COUNT("likeCount");

  @JsonValue
  private final String value;

  @JsonCreator
  public static CommentOrderBy from(String value) {
    if (value == null || value.isBlank()) {
      return CREATED_AT;
    }
    return Arrays.stream(values())
        .filter(orderBy -> orderBy.value.equalsIgnoreCase(value) || orderBy.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TYPE_VALUE));
  }
}
