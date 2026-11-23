package com.sprint.mission.sb15monewteam02;

import com.sprint.mission.sb15monewteam02.article.entity.Article;
import com.sprint.mission.sb15monewteam02.article.repository.ArticleRepository;
import com.sprint.mission.sb15monewteam02.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ArticleServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @InjectMocks
    ArticleService articleService;

    @Test
    void 중복된_기사는_저장하지_않는다() {

      // given: 이미 저장된 기사의 URL
      String url = "https://news.example.com/1";

      when(articleRepository.existsBySourceUrl(url))
          .thenReturn(true);

      // when: 같은 URL의 기사 저장 시도
      boolean saved = articleService.saveIfNew(url);

      // then: 저장되지 않아야 한다
      assertFalse(saved);

      verify(articleRepository, never())
          .save(any(Article.class));
    }
}
