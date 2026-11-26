package com.sprint.mission.sb15monewteam02.common.interest.controller;

import com.sprint.mission.sb15monewteam02.common.interest.dto.InterestDto;
import com.sprint.mission.sb15monewteam02.common.interest.dto.InterestRegisterRequest;
import com.sprint.mission.sb15monewteam02.common.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.common.interest.service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    // 관심사 등록(register) api
    @PostMapping
    public ResponseEntity<InterestDto> register(@RequestBody InterestRegisterRequest request) {
        Interest interest = interestService.register(request.name(), request.keywords());
        return ResponseEntity.status(HttpStatus.CREATED).body(InterestDto.from(interest));
    }


    // 관심사 삭제
    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> delete(@PathVariable UUID interestId) {
        interestService.delete(interestId);
        return ResponseEntity.noContent().build();
    }
}
