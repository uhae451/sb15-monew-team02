package com.sprint.mission.sb15monewteam02.article.collection.batch;

import com.sprint.mission.sb15monewteam02.article.collection.service.NewsCollectionService;
import com.sprint.mission.sb15monewteam02.article.collection.service.NewsInterestReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectionTasklet implements Tasklet {

  private final NewsInterestReader newsInterestReader;
  private final NewsCollectionService newsCollectionService;

  // RSS 뉴스 수집 대상
  private static final Map<String, String> RSS_SOURCES = Map.of(
      "한국경제", "https://www.hankyung.com/feed/all-news",
      "조선일보", "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml",
      "연합뉴스", "https://www.yonhapnewstv.co.kr/browse/feed/"
  );

  // 뉴스 수집 배치 작업 실행
  @Override
  public RepeatStatus execute(
      StepContribution contribution,
      ChunkContext chunkContext
  ) {
    Map<UUID, List<String>> interestKeywords =
        newsInterestReader.read();

    int savedCount = newsCollectionService.collectAndSave(
        interestKeywords, RSS_SOURCES
    );

    contribution.incrementWriteCount(savedCount);

    log.info("뉴스 수집 작업 종료: 신규 기사 {}건", savedCount);

    return RepeatStatus.FINISHED;
  }
}