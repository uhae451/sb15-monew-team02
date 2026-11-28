package com.sprint.mission.sb15monewteam02.interest.repository;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;

import java.util.List;

public interface InterestRepositoryCustom {

    List<Interest> searchInterests(InterestSearchCondition condition);
}