package com.sprint.mission.sb15monewteam02.comment.port;

import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Fallback
public class DefaultUserReader implements UserReader {

  @Override
  public Optional<UserSummary> findActiveUser(UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }
    return Optional.of(new UserSummary(userId, defaultNickname(userId)));
  }

  @Override
  public Map<UUID, UserSummary> findUsersByIds(Collection<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }
    return userIds.stream()
        .distinct()
        .map(id -> new UserSummary(id, defaultNickname(id)))
        .collect(Collectors.toMap(UserSummary::id, Function.identity()));
  }

  private String defaultNickname(UUID userId) {
    return "user-" + userId.toString().substring(0, 8);
  }
}
