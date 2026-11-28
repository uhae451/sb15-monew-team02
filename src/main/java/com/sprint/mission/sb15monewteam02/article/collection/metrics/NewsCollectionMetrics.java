package com.sprint.mission.sb15monewteam02.article.collection.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NewsCollectionMetrics {

  private final Counter executionCounter;
  private final Counter successCounter;
  private final Counter failureCounter;
  private final Counter savedArticleCounter;
  private final Timer executionTimer;

  public NewsCollectionMetrics(MeterRegistry registry) {

    executionCounter = registry.counter(
        "news.collection.execution"
    );

    successCounter = registry.counter(
        "news.collection.success"
    );

    failureCounter = registry.counter(
        "news.collection.failure"
    );

    savedArticleCounter = registry.counter(
        "news.collection.saved"
    );

    executionTimer = registry.timer(
        "news.collection.duration"
    );
  }

  // 실행 횟수 기록
  public void recordExecution() {
    executionCounter.increment();
  }

  // 성공 횟수 기록
  public void recordSuccess() {
    successCounter.increment();
  }

  // 실패 횟수 기록
  public void recordFailure() {
    failureCounter.increment();
  }

  // 신규 기사 저장 건수 기록
  public void recordSavedArticles(long count) {
    savedArticleCounter.increment(count);
  }

  // 실행 소요 시간 기록
  public void recordDuration(Duration duration) {
    executionTimer.record(duration);
  }
}