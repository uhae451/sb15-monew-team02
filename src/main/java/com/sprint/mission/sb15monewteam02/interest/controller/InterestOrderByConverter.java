package com.sprint.mission.sb15monewteam02.interest.controller;

import com.sprint.mission.sb15monewteam02.interest.repository.InterestOrderBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// 쿼리 파라미터 orderBy(name, subscriberCount)를 InterestOrderBy로 변환
@Component
public class InterestOrderByConverter implements Converter<String, InterestOrderBy> {

    @Override
    public InterestOrderBy convert(String source) {
        return InterestOrderBy.from(source);
    }
}
