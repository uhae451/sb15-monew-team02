package com.sprint.mission.sb15monewteam02;


import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.common.exception.DuplicateEmailException;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
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
                .willThrow(new DuplicateEmailException(DUPLICATE_EMAIL));

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
}