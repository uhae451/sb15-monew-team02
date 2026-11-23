package com.sprint.mission.sb15monewteam02.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageResponseTest {

    @Test
    @DisplayName("CursorPageResponse.of 생성 시 content, 커서 정보, 크기, 총 개수, hasNext가 정확히 설정된다")
    void createsCursorPageResponse() {
        CursorPageResponse<String> response = CursorPageResponse.of(
                List.of("a", "b"),
                "cursor-1",
                "2026-11-23T00:00:00Z",
                5L,
                true
        );

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.nextCursor()).isEqualTo("cursor-1");
        assertThat(response.nextAfter()).isEqualTo("2026-11-23T00:00:00Z");
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지인 경우 nextCursor와 nextAfter는 null이고 hasNext는 false이다")
    void lastPageHasNullCursors() {
        CursorPageResponse<String> response = CursorPageResponse.of(
                List.of("a"),
                null,
                null,
                1L,
                false
        );

        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextAfter()).isNull();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("SortDirection enum은 ASC와 DESC를 포함한다")
    void sortDirectionValues() {
        assertThat(SortDirection.values()).containsExactly(SortDirection.ASC, SortDirection.DESC);
    }
}
