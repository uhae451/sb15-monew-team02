package com.sprint.mission.sb15monewteam02.article.collection.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "monew.news.scheduler.enabled",
    havingValue = "true"
)
public class NewsCollectionScheduler {

  private final JobOperator jobOperator;
  private final Job newsCollectionJob;

  public NewsCollectionScheduler(
      JobOperator jobOperator,
      @Qualifier("newsCollectionJob") Job newsCollectionJob
  ) {
    this.jobOperator = jobOperator;
    this.newsCollectionJob = newsCollectionJob;
  }

  // 매시간 정각에 뉴스 수집 작업 실행
  @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
  public void collectNews() {

    JobParameters parameters = new JobParametersBuilder()
        .addString("collectionHour",
            Instant.now().truncatedTo(ChronoUnit.HOURS).toString())
        .toJobParameters();

    try {
      jobOperator.start(newsCollectionJob, parameters);
    } catch (Exception e) {
      log.error("뉴스 수집 배치 실행 실패", e);
    }
  }
}