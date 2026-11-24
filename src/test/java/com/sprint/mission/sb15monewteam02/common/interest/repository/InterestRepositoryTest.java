package com.sprint.mission.sb15monewteam02.common.interest.repository;

import com.sprint.mission.sb15monewteam02.common.interest.entity.Interest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// 로컬 DB로 테스트
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InterestRepositoryTest {

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 여러_키워드를_저장한_뒤_다시_조회할_수_있다() {

        // given
        Interest saved = interestRepository.save(
                Interest.create("인공지능", List.of("AI", "머신러닝", "딥러닝")));

        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트를 비워서 DB에서 다시 읽도록 함

        // then
        Interest found = interestRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getKeywords())
                .containsExactlyInAnyOrder("AI", "머신러닝", "딥러닝");
    }
}