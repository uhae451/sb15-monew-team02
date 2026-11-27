package com.sprint.mission.sb15monewteam02.interest.repository;

import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        // 로컬 도커 Postgres(monew-postgres, 5433) 접속 정보 - 이 테스트에서만 덮어씀
        "spring.datasource.url=jdbc:postgresql://localhost:5433/postgres",
        "spring.datasource.username=postgres",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
// 로컬 DB로 테스트
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InterestRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InterestRepository interestRepository;

    private InterestSearchCondition condition(String keyword, InterestOrderBy orderBy, Sort.Direction direction) {
        return new InterestSearchCondition(keyword, orderBy, direction, null, null, 20);
    }

    @Test
    void 이름_일부로_검색하면_해당_관심사를_찾는다() {
        em.persist(Interest.create("인공지능", List.of("AI")));
        em.persist(Interest.create("경제", List.of("주식")));
        em.flush();
        em.clear();

        List<Interest> result = interestRepository.searchInterests(
                condition("지능", InterestOrderBy.NAME, Sort.Direction.ASC));

        assertThat(result).extracting(Interest::getName).containsExactly("인공지능");
    }

    @Test
    void 키워드_일부로_검색하면_해당_관심사를_찾는다() {
        em.persist(Interest.create("인공지능", List.of("AI")));
        em.persist(Interest.create("경제", List.of("머신러닝")));
        em.flush();
        em.clear();

        List<Interest> result = interestRepository.searchInterests(
                condition("머신", InterestOrderBy.NAME, Sort.Direction.ASC));

        assertThat(result).extracting(Interest::getName).containsExactly("경제");
    }

    @Test
    void 이름과_키워드가_모두_맞아도_한_번만_조회된다() {
        em.persist(Interest.create("AI뉴스", List.of("AI", "인공지능")));
        em.flush();
        em.clear();

        List<Interest> result = interestRepository.searchInterests(
                condition("AI", InterestOrderBy.NAME, Sort.Direction.ASC));

        assertThat(result).hasSize(1);
    }

    @Test
    void 검색어가_없으면_이름순으로_전체를_조회한다() {
        em.persist(Interest.create("나", List.of("k")));
        em.persist(Interest.create("가", List.of("k")));
        em.persist(Interest.create("다", List.of("k")));
        em.flush();
        em.clear();

        List<Interest> result = interestRepository.searchInterests(
                condition(null, InterestOrderBy.NAME, Sort.Direction.ASC));

        assertThat(result).extracting(Interest::getName).containsExactly("가", "나", "다");
    }
}