package com.sprint.mission.sb15monewteam02.interest.repository;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID>, InterestRepositoryCustom {

    // 구독자 수를 DB에서 직접 +1 (엔티티를 읽고 더해서 저장하면 동시 요청 시 값이 덮어써질 수 있음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1 WHERE i.id = :interestId")
    int incrementSubscriberCount(@Param("interestId") UUID interestId);

    // 구독자 수를 DB에서 직접 -1 (0 아래로는 내려가지 않음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 WHERE i.id = :interestId AND i.subscriberCount > 0")
    int decrementSubscriberCount(@Param("interestId") UUID interestId);
}
