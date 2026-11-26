package com.sprint.mission.sb15monewteam02.common.interest.repository;

import com.sprint.mission.sb15monewteam02.common.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID> {
}
