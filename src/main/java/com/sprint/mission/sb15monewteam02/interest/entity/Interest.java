package com.sprint.mission.sb15monewteam02.interest.entity;

import com.sprint.mission.sb15monewteam02.common.exception.InterestKeywordEmptyException;
import com.sprint.mission.sb15monewteam02.common.exception.InterestNameBlankException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest {

    @Id
    // id 타입 UUID: API 명세서 참고
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private long subscriberCount;

    @ElementCollection
    // 한 칸에 여러 값을 넣을 수 없어서 별도 테이블 생성
    @CollectionTable(name = "interest_keywords")
    // 키워드는 여러개를 가질 수 있다: 요구사항
    @Column(name = "keyword", nullable = false)
    private List<String> keywords = new ArrayList<>();

    private Interest(String name, List<String> keywords) {
        this.name = name;
        // 전달받은 리스트를 그대로 대입하지 않고, 내용만 복사해서 대입
        this.keywords.addAll(keywords);
    }

    // 키워드 검증
    private static void validateKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new InterestKeywordEmptyException();
        }
    }

    // 관심사 생성
    public static Interest create(String name, List<String> keywords) {
        if (name == null || name.isBlank()) {
            throw new InterestNameBlankException();
        }
        validateKeywords(keywords);
        return new Interest(name, keywords);
    }

    // 관심사 수정
    public void updateKeywords(List<String> newKeywords) {
        validateKeywords(newKeywords);

        // 기존 리스트 초기화 후 새 키워드로 교체
        this.keywords.clear();
        this.keywords.addAll(newKeywords);
    }

    // 조회용 키워드 목록: 외부에서 수정하지 못하도록 복사본 반환
    public List<String> getKeywords() {
        return List.copyOf(keywords);
    }
}