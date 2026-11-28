package com.sprint.mission.sb15monewteam02.article.collection.batch;

import com.sprint.mission.sb15monewteam02.article.collection.service.NewsCollectionService;
import com.sprint.mission.sb15monewteam02.article.collection.service.NewsInterestReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsCollectionTaskletTest {

  @Mock
  private NewsInterestReader newsInterestReader;

  @Mock
  private NewsCollectionService newsCollectionService;

  @Mock
  private StepContribution contribution;

  @Mock
  private ChunkContext chunkContext;

  @InjectMocks
  private NewsCollectionTasklet tasklet;

  @Test
  void 뉴스_수집_배치_정상_실행() {

    // given
    Map<UUID, List<String>> interests =
        Map.of(UUID.randomUUID(), List.of("인공지능"));

    when(newsInterestReader.read()).thenReturn(interests);

    when(newsCollectionService.collectAndSave(
        eq(interests), anyMap()
    )).thenReturn(5);

    // when
    RepeatStatus result =
        tasklet.execute(contribution, chunkContext);

    // then
    assertEquals(RepeatStatus.FINISHED, result);

    verify(newsInterestReader).read();

    verify(newsCollectionService)
        .collectAndSave(eq(interests), anyMap());

    verify(contribution).incrementWriteCount(5);
  }

  @Test
  void 뉴스_수집_실패시_예외_전달() {

    // given
    when(newsInterestReader.read())
        .thenThrow(new IllegalStateException("조회 실패"));

    // when & then
    assertThrows(IllegalStateException.class,
        () -> tasklet.execute(contribution, chunkContext));

    verifyNoInteractions(newsCollectionService);
  }
}