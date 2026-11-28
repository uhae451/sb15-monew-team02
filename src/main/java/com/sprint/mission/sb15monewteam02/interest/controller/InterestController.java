package com.sprint.mission.sb15monewteam02.interest.controller;

import com.sprint.mission.sb15monewteam02.interest.dto.GetInterestResponse;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestDto;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestRegisterRequest;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestUpdateRequest;
import com.sprint.mission.sb15monewteam02.interest.dto.SubscriptionDto;
import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestOrderBy;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestSearchCondition;
import com.sprint.mission.sb15monewteam02.interest.service.InterestService;
import com.sprint.mission.sb15monewteam02.interest.service.SubscriptionService;
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

    private static final String REQUEST_USER_HEADER = "Monew-Request-User-ID";

    private final InterestService interestService;
    private final SubscriptionService subscriptionService;

    // 관심사 등록(register) api
    @PostMapping
    public ResponseEntity<InterestDto> register(@RequestBody InterestRegisterRequest request) {
        Interest interest = interestService.register(request.name(), request.keywords());
        return ResponseEntity.status(HttpStatus.CREATED).body(InterestDto.from(interest));
    }


    // 관심사 키워드 수정
    @PatchMapping("/{interestId}")
    public ResponseEntity<InterestDto> updateKeywords(
            @PathVariable UUID interestId,
            @RequestBody InterestUpdateRequest request
    ) {
        Interest interest = interestService.updateKeywords(interestId, request.keywords());
        return ResponseEntity.ok(InterestDto.from(interest));
    }

    // 관심사 삭제
    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> delete(@PathVariable UUID interestId) {
        interestService.delete(interestId);
        return ResponseEntity.noContent().build();
    }

    // 관심사 구독
    @PostMapping("/{interestId}/subscriptions")
    public ResponseEntity<SubscriptionDto> subscribe(
            @PathVariable UUID interestId,
            @RequestHeader(REQUEST_USER_HEADER) UUID userId
    ) {
        return ResponseEntity.ok(subscriptionService.subscribe(interestId, userId));
    }

    // 관심사 구독 취소
    @DeleteMapping("/{interestId}/subscriptions")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable UUID interestId,
            @RequestHeader(REQUEST_USER_HEADER) UUID userId
    ) {
        subscriptionService.unsubscribe(interestId, userId);
        return ResponseEntity.ok().build();
    }

    // 관심사 조회
    @GetMapping
    public ResponseEntity<GetInterestResponse> getInterests(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "name") InterestOrderBy orderBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID after,
            @RequestParam(defaultValue = "20") int limit,
            // subscribedByMe 계산용. 없으면 모두 false로 응답
            @RequestHeader(value = REQUEST_USER_HEADER, required = false) UUID userId
    ) {
        InterestSearchCondition condition =
                new InterestSearchCondition(keyword, orderBy, direction, cursor, after, limit);
        return ResponseEntity.ok(interestService.search(condition, userId));
    }
}
