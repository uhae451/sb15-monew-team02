package com.sprint.mission.sb15monewteam02.comment.port;

import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Fallback
public class DefaultArticleReader implements ArticleReader {

    @Override
    public Optional<ArticleSummary> findActiveArticle(UUID articleId) {
        if (articleId == null) {
            return Optional.empty();
        }
        return Optional.of(new ArticleSummary(articleId, "Article " + articleId.toString().substring(0, 8)));
    }
}
