package com.sprint.mission.sb15monewteam02.common.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        String nextAfter,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static <T> CursorPageResponse<T> of(
            List<T> content,
            String nextCursor,
            String nextAfter,
            long totalElements,
            boolean hasNext
    ) {
        return new CursorPageResponse<>(
                content,
                hasNext ? nextCursor : null,
                hasNext ? nextAfter : null,
                content.size(),
                totalElements,
                hasNext
        );
    }
}
