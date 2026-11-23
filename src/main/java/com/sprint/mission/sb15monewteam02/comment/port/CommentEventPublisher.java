package com.sprint.mission.sb15monewteam02.comment.port;

import com.sprint.mission.sb15monewteam02.comment.event.CommentCreatedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentDeletedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentHardDeletedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentLikedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentUnlikedEvent;
import com.sprint.mission.sb15monewteam02.comment.event.CommentUpdatedEvent;

public interface CommentEventPublisher {

  void publishCommentCreated(CommentCreatedEvent event);

  void publishCommentUpdated(CommentUpdatedEvent event);

  void publishCommentDeleted(CommentDeletedEvent event);

  void publishCommentHardDeleted(CommentHardDeletedEvent event);

  void publishCommentLiked(CommentLikedEvent event);

  void publishCommentUnliked(CommentUnlikedEvent event);
}
