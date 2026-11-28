package com.sprint.mission.sb15monewteam02.interest.service;

import com.sprint.mission.sb15monewteam02.common.exception.InterestNameSimilarException;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNotFoundException;
import com.sprint.mission.sb15monewteam02.interest.dto.GetInterestResponse;
import com.sprint.mission.sb15monewteam02.interest.dto.InterestDto;
import com.sprint.mission.sb15monewteam02.interest.entity.Interest;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestOrderBy;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestRepository;
import com.sprint.mission.sb15monewteam02.interest.repository.InterestSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InterestService {

    private static final double SIMILAR_THRESHOLD = 0.8;

    private final InterestRepository interestRepository;

    // 관심사 등록
    public Interest register(String name, List<String> keywords) {

        Interest newInterest = Interest.create(name, keywords); // 검증 먼저
        boolean tooSimilar = interestRepository.findAll().stream()
                .anyMatch(existing -> isSimilar(existing.getName(), name));
        if (tooSimilar) {
            throw new InterestNameSimilarException();
        }
        return interestRepository.save(Interest.create(name, keywords));
    }

    // 관심사 삭제
    public void delete(UUID id) {
        Interest interest = interestRepository.findById(id)
                .orElseThrow(InterestNotFoundException::new);
        interestRepository.delete(interest);
    }

    @Transactional(readOnly = true)
    public GetInterestResponse search(InterestSearchCondition condition) {

        // Limit + 1을 가져와서 다음 페이지가 있는지 계산
        InterestSearchCondition fetchCondition = new InterestSearchCondition(
                condition.keyword(), condition.orderBy(), condition.direction(),
                condition.cursor(), condition.after(), condition.limit() + 1
        );
        List<Interest> rows = interestRepository.searchInterests(fetchCondition);

        boolean hasNext = rows.size() > condition.limit();
        List<Interest> page = hasNext ? rows.subList(0, condition.limit()) : rows;

        List<InterestDto> content = page.stream().map(InterestDto::from).toList();

        String nextCursor = null;
        String nextAfter = null;
        if (hasNext) {
            Interest last = page.get(page.size() - 1);
            nextCursor = condition.orderBy() == InterestOrderBy.SUBSCRIBER_COUNT
                    ? String.valueOf(last.getSubscriberCount())
                    : last.getName();
            nextAfter = last.getId().toString();
        }

        // TODO: 검색 조건을 반영한 count 쿼리로 바꿔야 함 (지금은 전체 개수)
        long totalElements = interestRepository.count();

        return new GetInterestResponse(content, nextCursor, nextAfter, content.size(), totalElements, hasNext);
    }

    // 키워드 수정
    public Interest updateKeywords(UUID id, List<String> keywords) {
        Interest interest = interestRepository.findById(id)
                .orElseThrow(InterestNotFoundException::new);

        interest.updateKeywords(keywords);
        return interest;
    }

    private boolean isSimilar(String a, String b) {
        return calculateSimilarity(normalize(a), normalize(b)) >= SIMILAR_THRESHOLD;
    }

    // 유사도 비교 전 정규화: 앞뒤 공백 제거 + 소문자 변환 ("ECONOMY", " Economy " → "economy")
    private String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    // 유사도 80% 검증
    // 유사도 = 1 - (편집 거리 / 더 긴 문자열 길이)
    private double calculateSimilarity(String a, String b) {
        int maxLength = Math.max(a.length(), b.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - (double) levenshteinDistance(a, b) / maxLength;
    }

    // 한 글자를 추가·삭제·교체하는 최소 횟수
    // 레벤슈타인 알고리즘
    private int levenshteinDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[b.length()];
    }
}