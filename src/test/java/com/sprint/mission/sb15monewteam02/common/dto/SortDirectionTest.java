package com.sprint.mission.sb15monewteam02.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SortDirectionTest {

  @Test
  @DisplayName("SortDirection enum은 ASC와 DESC를 포함한다")
  void sortDirectionValues() {
    assertThat(SortDirection.values()).containsExactly(SortDirection.ASC, SortDirection.DESC);
  }
}
