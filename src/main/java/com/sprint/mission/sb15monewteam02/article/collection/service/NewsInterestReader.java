package com.sprint.mission.sb15monewteam02.article.collection.service;

import com.sprint.mission.sb15monewteam02.common.interest.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NewsInterestReader {

  private final InterestRepository interestRepository;

  // 등록된 관심사의 ID와 키워드를 조회한다.
  @Transactional(readOnly = true)
  public Map<UUID, List<String>> read() {

    Map<UUID, List<String>> result = new HashMap<>();

    interestRepository.findAll().forEach(interest ->
        result.put(interest.getId(), interest.getKeywords())
    );

    return result;
  }
}