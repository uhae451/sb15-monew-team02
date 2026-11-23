package com.sprint.mission.sb15monewteam02.comment.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserReader {

    Optional<UserSummary> findActiveUser(UUID userId);

    Map<UUID, UserSummary> findUsersByIds(Collection<UUID> userIds);
}
