package com.sprint.mission.sb15monewteam02.common.interest;

import com.sprint.mission.sb15monewteam02.common.exception.InterestKeywordEmptyException;
import com.sprint.mission.sb15monewteam02.common.interest.entity.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class InterestTest {

    List<String> input;
    Interest interest;
    @BeforeEach
    void setup(){
         input = new ArrayList<>(List.of("AI"));
        interest = Interest.create("인공지능", input);
    }


    @Test
    void 생성_이후_전달받은_리스트를_바꿔도_키워드는_변하지_않는다() {

        // when
        input.add("머신러닝");
        // then
        assertThat(interest.getKeywords()).containsExactly("AI");
    }

    @Test
    void 조회한_키워드_목록을_외부에서_수정할_수_없다() {

        // when&then
        assertThatThrownBy(() -> interest.getKeywords().add("머신러닝"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 잘못된_키워드로_수정해도_기존_키워드는_유지된다() {

        // when
        assertThatThrownBy(() -> interest.updateKeywords(List.of()))
                .isInstanceOf(InterestKeywordEmptyException.class);

        // then
        assertThat(interest.getKeywords()).containsExactly("AI");
    }

    @Test
    void 생성_직후_구독자_수는_0이다() {

        // when&then
        assertThat(interest.getSubscriberCount()).isZero();
    }
}