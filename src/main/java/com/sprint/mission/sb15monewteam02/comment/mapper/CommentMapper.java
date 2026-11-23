package com.sprint.mission.sb15monewteam02.comment.mapper;

import com.sprint.mission.sb15monewteam02.comment.dto.CommentLikeResponse;
import com.sprint.mission.sb15monewteam02.comment.dto.CommentResponse;
import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import com.sprint.mission.sb15monewteam02.comment.entity.CommentLike;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment, String userNickname, boolean likedByMe) {
        return new CommentResponse(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                userNickname,
                comment.getContent(),
                comment.getLikeCount(),
                likedByMe,
                comment.getCreatedAt()
        );
    }

    public CommentLikeResponse toLikeResponse(
            CommentLike commentLike,
            Comment comment,
            String articleTitle,
            String commentUserNickname,
            long commentLikeCount
    ) {
        return new CommentLikeResponse(
                commentLike.getId(),
                commentLike.getUserId(),
                commentLike.getCreatedAt(),
                comment.getId(),
                comment.getArticleId(),
                articleTitle,
                comment.getUserId(),
                commentUserNickname,
                comment.getContent(),
                commentLikeCount,
                comment.getCreatedAt()
        );
    }
}
