package com.sprint.mission.sb15monewteam02;


import com.sprint.mission.sb15monewteam02.newsapp.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.newsapp.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.newsapp.user.entity.User;
import com.sprint.mission.sb15monewteam02.newsapp.user.exception.DuplicateEmailException;
import com.sprint.mission.sb15monewteam02.newsapp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * UserService 단위 테스트
 * - 스프링을 띄우지 않고, Repository는 가짜(Mock)로 대체해서 Service 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;   // 가짜 Repository (DB에 접근하지 않음)

    @InjectMocks
    private UserService userService;         // 위 가짜 Repository를 주입받은 진짜 Service

    @Nested
    @DisplayName("회원가입")
    class Create {

        private final UserCreateRequest request =
                new UserCreateRequest("test@example.com", "테스터", "password123");

        @Test
        @DisplayName("성공하면 사용자를 저장하고 UserDto를 반환한다")
        void success() {
            // given: 중복 이메일이 없고, save는 받은 객체를 그대로 돌려준다고 가정
            given(userRepository.existsByEmail("test@example.com")).willReturn(false);
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            UserDto result = userService.create(request);

            // then
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.nickname()).isEqualTo("테스터");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("저장되는 User에는 요청값이 그대로 담기고, 삭제 시각은 비어 있다")
        void savedUserHasRequestValues() {
            // given
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            userService.create(request);

            // then: save()에 실제로 넘어간 User 객체를 붙잡아서 내용 확인
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(captor.capture());
            User savedUser = captor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(savedUser.getNickname()).isEqualTo("테스터");
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getDeletedAt()).isNull();   // 가입 직후는 활성 사용자
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 DuplicateEmailException이 발생하고 저장하지 않는다")
        void fail_duplicateEmail() {
            // given
            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(DuplicateEmailException.class);

            then(userRepository).should(never()).save(any());
        }
    }
}