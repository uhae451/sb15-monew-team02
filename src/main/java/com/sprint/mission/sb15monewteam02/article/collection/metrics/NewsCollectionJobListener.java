package com.sprint.mission.sb15monewteam02.article.collection.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class NewsCollectionJobListener implements JobExecutionListener {

  private final NewsCollectionMetrics metrics;

  // 배치 실행 전 실행 횟수를 기록한다.
  @Override
  public void beforeJob(JobExecution jobExecution) {
    metrics.recordExecution();
  }

  // 배치 종료 후 실행 결과를 기록한다.
  @Override
  public void afterJob(JobExecution jobExecution) {

    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      metrics.recordSuccess();
    } else {
      metrics.recordFailure();
    }

    long savedCount = jobExecution.getStepExecutions()
        .stream()
        .mapToLong(StepExecution::getWriteCount)
        .sum();

    metrics.recordSavedArticles(savedCount);

    if (jobExecution.getStartTime() != null
        && jobExecution.getEndTime() != null) {

      metrics.recordDuration(Duration.between(
          jobExecution.getStartTime(),
          jobExecution.getEndTime()
      ));
    }
  }
}