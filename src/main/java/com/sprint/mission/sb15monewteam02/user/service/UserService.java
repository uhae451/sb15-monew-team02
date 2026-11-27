package com.sprint.mission.sb15monewteam02.user.service;

import com.sprint.mission.sb15monewteam02.common.exception.DuplicateEmailException;
import com.sprint.mission.sb15monewteam02.common.exception.InvalidCredentialsException;
import com.sprint.mission.sb15monewteam02.common.exception.UserNotFoundException;
import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.user.dto.UserLoginRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserUpdateRequest;
import com.sprint.mission.sb15monewteam02.user.entity.User;
import com.sprint.mission.sb15monewteam02.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.sprint.mission.sb15monewteam02.common.exception.ErrorCode.DUPLICATE_EMAIL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    //사용자 회원 가입 서비스 구현
    @Transactional
    public UserDto create(UserCreateRequest request){
        // 이미 있는 이메일일 때 예외처리
        if (userRepository.existsByEmail(request.email())){
            throw new DuplicateEmailException();
        }
        //객체 생성 및 저장.
        User user = new User(request.email(), request.nickname(), request.password());
        User saved = userRepository.save(user);
        return UserDto.from(saved);
    }
    // 로그인 서비스 구현
    public UserDto login(UserLoginRequest request) {
        // 객체 조회
        Optional<User> found = userRepository.findByEmail(request.email());
        // 이메일 없으면 예외처리
        if (found.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        // 삭제된 사용자가 있으면 예외처리
        if (user.isDeleted()) {
            throw new InvalidCredentialsException();
        }
        // 비밀번호가 같지 않을시 예외처리
        if (!user.getPassword().equals(request.password())) {
            throw new InvalidCredentialsException();
        }
        return UserDto.from(user);
    }
    // 사용자 정보(닉네임) 수정
    @Transactional
    public UserDto update(UUID userId, UserUpdateRequest request) {
        User user = findActiveUser(userId);
        user.updateNickname(request.nickname());   // 변경 감지로 저장된다
        return UserDto.from(user);
    }
    // 사용자 논리 삭제
    @Transactional
    public void softDelete(UUID userId) {
        User user = findActiveUser(userId);
        user.delete();
    }
    // 사용자 물리 삭제 (논리 삭제된 사용자도 삭제 가능)
    @Transactional
    public void hardDelete(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        userRepository.delete(user);
    }
    // 논리 삭제되지 않은 사용자 조회
    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(UserNotFoundException::new);
    }
}
