package com.sprint.mission.sb15monewteam02.interest.repository;

import com.sprint.mission.sb15monewteam02.interest.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsByInterestIdAndUserId(UUID interestId, UUID userId);

    // 주어진 관심사들 중 사용자가 구독한 관심사 id만 조회 (subscribedByMe 계산용, 페이지당 쿼리 1번)
    @Query("SELECT s.interest.id FROM Subscription s WHERE s.userId = :userId AND s.interest.id IN :interestIds")
    Set<UUID> findSubscribedInterestIds(@Param("userId") UUID userId,
                                        @Param("interestIds") Collection<UUID> interestIds);

    // 구독 취소: 삭제된 행 수를 반환 (0이면 구독한 적 없음)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Subscription s WHERE s.interest.id = :interestId AND s.userId = :userId")
    int deleteByInterestIdAndUserId(@Param("interestId") UUID interestId, @Param("userId") UUID userId);

    // 관심사 삭제 시 해당 관심사의 구독 내역도 함께 삭제 (FK 제약 때문에 먼저 지워야 함)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Subscription s WHERE s.interest.id = :interestId")
    int deleteAllByInterestId(@Param("interestId") UUID interestId);
}
