package com.sprint.mission.sb15monewteam02.user.controller;

import com.sprint.mission.sb15monewteam02.user.dto.UserCreateRequest;
import com.sprint.mission.sb15monewteam02.user.dto.UserDto;
import com.sprint.mission.sb15monewteam02.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request){
        UserDto user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
