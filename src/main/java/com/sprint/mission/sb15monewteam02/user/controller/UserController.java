package com.sprint.mission.sb15monewteam02.user.controller;

import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.user.dto.UserLoginRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserUpdateRequest;
import com.sprint.mission.sb15monewteam02.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    //회원 가입 api
    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request){
        UserDto user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    //로그인 api
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
    //사용자 정보 수정 api
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDto> update(@PathVariable UUID userId,
                                          @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(userId, request));
    }
    //사용자 논리 삭제 api
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID userId) {
        userService.softDelete(userId);
        return ResponseEntity.noContent().build();
    }
}
