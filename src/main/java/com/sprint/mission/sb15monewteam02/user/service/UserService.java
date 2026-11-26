package com.sprint.mission.sb15monewteam02.user.service;

import com.sprint.mission.sb15monewteam02.common.exception.DuplicateEmailException;
import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.user.entity.User;
import com.sprint.mission.sb15monewteam02.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sprint.mission.sb15monewteam02.common.exception.ErrorCode.DUPLICATE_EMAIL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserDto create(UserCreateRequest request){
        if (userRepository.existsByEmail(request.email())){
            throw new DuplicateEmailException(DUPLICATE_EMAIL);
        }
        User user = new User(request.email(), request.nickname(), request.password());
        User saved = userRepository.save(user);
        return UserDto.from(saved);
    }
}
