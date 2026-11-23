package com.sprint.mission.sb15monewteam02;


import com.sprint.mission.sb15monewteam02.newsapp.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserRepository 테스트
 * - JPA 관련 부분만 띄우고, 테스트용 메모리 DB(H2)에서 실행한다.
 * - 각 테스트가 끝나면 자동으로 롤백되어 서로 영향을 주지 않는다.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("저장하면 id가 자동으로 생성된다")
    void save_generatesId() {
        User saved = userRepository.save(new User("test@example.com", "테스터", "password123"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("가입된 이메일이면 existsByEmail은 true를 반환한다")
    void existsByEmail_true() {
        userRepository.save(new User("test@example.com", "테스터", "password123"));

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 existsByEmail은 false를 반환한다")
    void existsByEmail_false() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    @DisplayName("같은 이메일을 두 번 저장하면 DB 유니크 제약 때문에 예외가 발생한다")
    void save_duplicateEmail_violatesUniqueConstraint() {
        userRepository.saveAndFlush(new User("test@example.com", "테스터1", "password123"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(new User("test@example.com", "테스터2", "password456")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}