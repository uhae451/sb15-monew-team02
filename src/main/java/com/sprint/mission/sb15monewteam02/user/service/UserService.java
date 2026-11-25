package com.sprint.mission.sb15monewteam02.user.service;

import com.sprint.mission.sb15monewteam02.common.exception.DuplicateEmailException;
import com.sprint.mission.sb15monewteam02.common.exception.InvalidCredentialsException;
import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.user.dto.UserLoginRequest;
import com.sprint.mission.sb15monewteam02.user.entity.User;
import com.sprint.mission.sb15monewteam02.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sprint.mission.sb15monewteam02.common.exception.ErrorCode.DUPLICATE_EMAIL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    //사용자 회원 가입 서비스 구현
    @Transactional
    public UserDto create(UserCreateRequest request){

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
}
