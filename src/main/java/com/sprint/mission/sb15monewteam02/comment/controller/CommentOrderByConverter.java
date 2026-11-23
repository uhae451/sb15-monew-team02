package com.sprint.mission.sb15monewteam02.comment.controller;

import com.sprint.mission.sb15monewteam02.comment.entity.CommentOrderBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CommentOrderByConverter implements Converter<String, CommentOrderBy> {

  @Override
  public CommentOrderBy convert(String source) {
    return CommentOrderBy.from(source);
  }
}
