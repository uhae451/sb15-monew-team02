package com.sprint.mission.sb15monewteam02.article.collection.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsCollectionSchedulerTest {

  @Mock
  private JobOperator jobOperator;

  @Mock
  private Job newsCollectionJob;

  @InjectMocks
  private NewsCollectionScheduler scheduler;

  @Test
  void 뉴스_수집_Job_정상_호출() throws Exception {

    // given
    Instant before = Instant.now().truncatedTo(ChronoUnit.HOURS);

    // when
    scheduler.collectNews();

    // then
    ArgumentCaptor<JobParameters> captor =
        ArgumentCaptor.forClass(JobParameters.class);

    verify(jobOperator).start(
        eq(newsCollectionJob), captor.capture()
    );

    Instant after = Instant.now().truncatedTo(ChronoUnit.HOURS);

    String collectionHour =
        captor.getValue().getString("collectionHour");

    assertTrue(
        before.toString().equals(collectionHour)
            || after.toString().equals(collectionHour)
    );
  }

  @Test
  void 배치_실행_실패시_예외_처리() throws Exception {

    // given
    when(jobOperator.start(
        eq(newsCollectionJob), any(JobParameters.class)
    )).thenThrow(new IllegalStateException("배치 실행 실패"));

    // when & then
    assertDoesNotThrow(() -> scheduler.collectNews());

    verify(jobOperator).start(
        eq(newsCollectionJob), any(JobParameters.class)
    );
  }
}