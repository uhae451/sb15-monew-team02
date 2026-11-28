package com.sprint.mission.sb15monewteam02.article.collection.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NewsCollectionMetricsTest {

  private final SimpleMeterRegistry registry =
      new SimpleMeterRegistry();

  private final NewsCollectionMetrics metrics =
      new NewsCollectionMetrics(registry);

  @Test
  void 배치_실행_횟수를_기록한다() {

    metrics.recordExecution();

    assertEquals(1.0,
        registry.get("news.collection.execution").counter().count());
  }

  @Test
  void 배치_성공과_실패_횟수를_기록한다() {

    metrics.recordSuccess();
    metrics.recordFailure();

    assertEquals(1.0,
        registry.get("news.collection.success").counter().count());

    assertEquals(1.0,
        registry.get("news.collection.failure").counter().count());
  }

  @Test
  void 신규_기사_저장_건수를_누적한다() {

    metrics.recordSavedArticles(5);
    metrics.recordSavedArticles(3);

    assertEquals(8.0,
        registry.get("news.collection.saved").counter().count());
  }

  @Test
  void 배치_실행_시간을_기록한다() {

    metrics.recordDuration(Duration.ofSeconds(2));

    assertEquals(2.0,
        registry.get("news.collection.duration")
            .timer().totalTime(TimeUnit.SECONDS));
  }
}