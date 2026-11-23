package com.sprint.mission.sb15monewteam02.comment.port;

import com.sprint.mission.sb15monewteam02.comment.event.CommentCreatedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentDeletedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentHardDeletedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentLikedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentUnlikedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
@RequiredArgsConstructor
public class SpringCommentEventPublisher implements CommentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishCommentCreated(CommentCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishCommentUpdated(CommentUpdatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishCommentDeleted(CommentDeletedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishCommentHardDeleted(CommentHardDeletedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishCommentLiked(CommentLikedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishCommentUnliked(CommentUnlikedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
