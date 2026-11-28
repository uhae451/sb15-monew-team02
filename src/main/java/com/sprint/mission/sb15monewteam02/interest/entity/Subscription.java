package com.sprint.mission.sb15monewteam02.interest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// 관심사 구독: 사용자 1명은 같은 관심사를 한 번만 구독할 수 있다.
@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                // 같은 사용자가 같은 관심사를 중복 구독하지 못하도록 DB에서도 막는다 (동시 요청 대비)
                @UniqueConstraint(name = "uk_subscriptions_interest_user", columnNames = {"interest_id", "user_id"})
        },
        indexes = {
                // 사용자별 구독 목록 조회용 (subscribedByMe, 활동 내역)
                @Index(name = "idx_subscriptions_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false, updatable = false)
    private Interest interest;

    // 다른 도메인(User)은 엔티티 대신 id로만 참조 (CommentLike와 동일한 방식)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Subscription(Interest interest, UUID userId) {
        this.interest = interest;
        this.userId = userId;
    }

    public static Subscription create(Interest interest, UUID userId) {
        return new Subscription(interest, userId);
    }

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
