package com.sprint.mission.sb15monewteam02;


import com.sprint.mission.sb15monewteam02.common.exception.InvalidCredentialsException;
import com.sprint.mission.sb15monewteam02.common.exception.UserNotFoundException;
import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.common.exception.DuplicateEmailException;
import com.sprint.mission.sb15monewteam02.user.dto.UserLoginRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserUpdateRequest;
import com.sprint.mission.sb15monewteam02.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static com.sprint.mission.sb15monewteam02.common.exception.ErrorCode.DUPLICATE_EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.sprint.mission.sb15monewteam02.user.controller.UserController;
/**
 * UserController 웹 계층 테스트
 * - Controller, 예외 처리(@RestControllerAdvice), Bean Validation만 띄운다.
 * - Service는 가짜(MockitoBean)로 대체한다.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;          // 실제 서버 없이 HTTP 요청을 흉내 내는 도구

    @MockitoBean
    private UserService userService;  // 스프링 컨테이너에 가짜 Service를 등록

    @Test
    @DisplayName("회원가입 성공 시 201 Created와 사용자 정보를 반환하고, 비밀번호는 응답에 없다")
    void create_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        given(userService.create(any(UserCreateRequest.class)))
                .willReturn(new UserDto(userId, "test@example.com", "테스터", Instant.now()));

        String body = """
                {
                  "email": "test@example.com",
                  "nickname": "테스터",
                  "password": "password123"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("테스터"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] email = \"{0}\"")
    @ValueSource(strings = {"", "not-an-email", "test@"})
    @DisplayName("이메일이 비어 있거나 형식이 잘못되면 400 Bad Request를 반환한다")
    void create_fail_invalidEmail(String invalidEmail) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "nickname": "테스터",
                  "password": "password123"
                }
                """.formatted(invalidEmail);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        // 검증에서 막혔으니 Service까지 가면 안 된다
        then(userService).should(never()).create(any());
    }

    @Test
    @DisplayName("닉네임이 없으면 400 Bad Request를 반환한다")
    void create_fail_missingNickname() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409 Conflict를 반환한다")
    void create_fail_duplicateEmail() throws Exception {
        // given: Service가 중복 예외를 던진다고 가정
        given(userService.create(any(UserCreateRequest.class)))
                .willThrow(new DuplicateEmailException());

        String body = """
                {
                  "email": "test@example.com",
                  "nickname": "테스터",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USR_001"));
    }

    @ParameterizedTest(name = "[{index}] nickname = \"{0}\"")
    @ValueSource(strings = {"a", "012345678901234567890"})
    @DisplayName("닉네임이 2자 미만이거나 20자를 넘으면 400 Bad Request를 반환한다")
    void create_fail_invalidNicknameSize(String invalidNickname) throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "nickname": "%s",
                  "password": "password123"
                }
                """.formatted(invalidNickname);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        then(userService).should(never()).create(any());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
    void create_fail_shortPassword() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "nickname": "테스터",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        then(userService).should(never()).create(any());
    }

    @Test
    @DisplayName("로그인 성공 시 200 OK와 사용자 정보를 반환하고, 비밀번호는 응답에 없다")
    void login_success() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userService.login(any(UserLoginRequest.class)))
                .willReturn(new UserDto(userId, "test@example.com", "테스터", Instant.now()));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "email": "test@example.com", "password": "password123" }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("로그인 실패 시 401과 USR_002 코드를 반환한다")
    void login_fail_invalidCredentials() throws Exception {
        given(userService.login(any())).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "email": "test@example.com", "password": "wrong-pw" }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USR_002"));
    }

    @ParameterizedTest(name = "[{index}] email = \"{0}\"")
    @ValueSource(strings = {"", "not-an-email"})
    @DisplayName("로그인 이메일이 비어 있거나 형식이 잘못되면 400을 반환한다")
    void login_fail_invalidEmail(String email) throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "email": "%s", "password": "password123" }
                            """.formatted(email)))
                .andExpect(status().isBadRequest());

        then(userService).should(never()).login(any());
    }

    @Test
    @DisplayName("닉네임 수정 성공 시 200 OK와 수정된 사용자 정보를 반환한다")
    void update_success() throws Exception {
        UUID userId = UUID.randomUUID();
        given(userService.update(eq(userId), any(UserUpdateRequest.class)))
                .willReturn(new UserDto(userId, "test@example.com", "새닉네임", Instant.now()));

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "nickname": "새닉네임" }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] nickname = \"{0}\"")
    @ValueSource(strings = {"", "a", "012345678901234567890"})
    @DisplayName("수정할 닉네임이 비어 있거나 길이가 맞지 않으면 400을 반환한다")
    void update_fail_invalidNickname(String nickname) throws Exception {
        mockMvc.perform(patch("/api/users/{userId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "nickname": "%s" }
                            """.formatted(nickname)))
                .andExpect(status().isBadRequest());

        then(userService).should(never()).update(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 수정하면 404와 USR_003 코드를 반환한다")
    void update_fail_userNotFound() throws Exception {
        given(userService.update(any(), any())).willThrow(new UserNotFoundException());

        mockMvc.perform(patch("/api/users/{userId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "nickname": "새닉네임" }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USR_003"));
    }
}
