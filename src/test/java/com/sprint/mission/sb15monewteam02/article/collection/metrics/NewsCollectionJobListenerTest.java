package com.sprint.mission.sb15monewteam02.article.collection.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;

import java.util.List;

import static org.mockito.Mockito.*;

class NewsCollectionJobListenerTest {

  private final NewsCollectionMetrics metrics =
      mock(NewsCollectionMetrics.class);

  private final NewsCollectionJobListener listener =
      new NewsCollectionJobListener(metrics);

  @Test
  void 배치_실행시_실행_횟수를_기록한다() {

    JobExecution execution = mock(JobExecution.class);

    listener.beforeJob(execution);

    verify(metrics).recordExecution();
  }

  @Test
  void 배치_성공시_성공_횟수를_기록한다() {

    JobExecution execution = mock(JobExecution.class);

    when(execution.getStatus())
        .thenReturn(BatchStatus.COMPLETED);

    when(execution.getStepExecutions())
        .thenReturn(List.of());

    listener.afterJob(execution);

    verify(metrics).recordSuccess();
    verify(metrics, never()).recordFailure();
  }

  @Test
  void 배치_실패시_실패_횟수를_기록한다() {

    JobExecution execution = mock(JobExecution.class);

    when(execution.getStatus())
        .thenReturn(BatchStatus.FAILED);

    when(execution.getStepExecutions())
        .thenReturn(List.of());

    listener.afterJob(execution);

    verify(metrics).recordFailure();
    verify(metrics, never()).recordSuccess();
  }
}