package com.sprint.mission.sb15monewteam02.article.collection.batch;

import com.sprint.mission.sb15monewteam02.article.collection.metrics.NewsCollectionJobListener;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NewsCollectionJobConfig {

  // 뉴스 수집 작업 단계 등록
  @Bean
  public Step newsCollectionStep(
      JobRepository jobRepository,
      NewsCollectionTasklet newsCollectionTasklet
  ) {
    return new StepBuilder("newsCollectionStep", jobRepository)
        .tasklet(newsCollectionTasklet)
        .build();
  }

  // 뉴스 수집 배치 작업 등록
  @Bean
  public Job newsCollectionJob(
      JobRepository jobRepository,
      @Qualifier("newsCollectionStep") Step newsCollectionStep,
      NewsCollectionJobListener listener
  ) {
    return new JobBuilder("newsCollectionJob", jobRepository)
        .listener(listener)
        .start(newsCollectionStep)
        .build();
  }
}