package com.sprint.mission.sb15monewteam02.article.exception;

public class DuplicateArticleException extends RuntimeException {
  public DuplicateArticleException() { super("An article with this source URL already exists"); }
}
