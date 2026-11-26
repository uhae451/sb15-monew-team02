package com.sprint.mission.sb15monewteam02.common.interest.repository;

import com.sprint.mission.sb15monewteam02.common.interest.entity.Interest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Sort;

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

//    @Test
//    void 이름에_검색어가_부분일치하는_관심사를_찾는다() {
//        interestRepository.save(Interest.create("인공지능", List.of("AI")));
//        interestRepository.save(Interest.create("경제", List.of("주식")));
//        entityManager.flush();
//        entityManager.clear();
//
//        List<Interest> result = interestRepository.searchInterests(
//                new InterestSearchCondition("지능", InterestOrderBy.NAME, Sort.Direction.ASC, null, null, 10));
//
//        assertThat(result).extracting(Interest::getName).containsExactly("인공지능");
//    }
//
//    @Test
//    void 키워드에_검색어가_부분일치하는_관심사를_찾는다() {
//        interestRepository.save(Interest.create("인공지능", List.of("AI")));
//        interestRepository.save(Interest.create("경제", List.of("머신러닝")));
//        entityManager.flush();
//        entityManager.clear();
//
//        List<Interest> result = interestRepository.searchInterests(
//                new InterestSearchCondition("머신", InterestOrderBy.NAME, Sort.Direction.ASC, null, null, 10));
//
//        assertThat(result).extracting(Interest::getName).containsExactly("경제");
//    }
//
//    @Test
//    void 이름과_키워드가_모두_맞아도_관심사는_한_번만_조회된다() {
//        interestRepository.save(Interest.create("AI뉴스", List.of("AI", "인공지능")));
//        entityManager.flush();
//        entityManager.clear();
//
//        List<Interest> result = interestRepository.searchInterests(
//                new InterestSearchCondition("AI", InterestOrderBy.NAME, Sort.Direction.ASC, null, null, 10));
//
//        assertThat(result).hasSize(1);
//    }
//
//    @Test
//    void 검색어가_없으면_이름순으로_모두_조회한다() {
//        interestRepository.save(Interest.create("나", List.of("a")));
//        interestRepository.save(Interest.create("가", List.of("b")));
//        interestRepository.save(Interest.create("다", List.of("c")));
//        entityManager.flush();
//        entityManager.clear();
//
//        List<Interest> result = interestRepository.searchInterests(
//                new InterestSearchCondition(null, InterestOrderBy.NAME, Sort.Direction.ASC, null, null, 10));
//
//        assertThat(result).extracting(Interest::getName).containsExactly("가", "나", "다");
//    }
}