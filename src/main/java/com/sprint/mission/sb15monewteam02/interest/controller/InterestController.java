package com.sprint.mission.sb15monewteam02.interest.controller;

import com.sprint.mission.sb15monewteam02.interest.dto.GetInterestResponse;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestDto;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestRegisterRequest;
import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestOrderBy;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestSearchCondition;
import com.sprint.mission.sb15monewteam02.interest.service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

    // 관심사 조회
    @GetMapping
    public ResponseEntity<GetInterestResponse> getInterests(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "NAME") InterestOrderBy orderBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID after,
            @RequestParam(defaultValue = "20") int limit
    ) {
        InterestSearchCondition condition =
                new InterestSearchCondition(keyword, orderBy, direction, cursor, after, limit);
        return ResponseEntity.ok(interestService.search(condition));
    }
}
