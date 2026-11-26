package com.sprint.mission.sb15monewteam02.comment.service;

import com.sprint.mission.sb15monewteam02.comment.entity.Comment;
import com.sprint.mission.sb15monewteam02.comment.exception.CommentLikeNotFoundException;
import com.sprint.mission.sb15monewteam02.comment.exception.DuplicateCommentLikeException;
import com.sprint.mission.sb15monewteam02.comment.repository.CommentLikeRepository;
import com.sprint.mission.sb15monewteam02.comment.repository.CommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CommentLikeConcurrencyTest {

  @Autowired
  private CommentService commentService;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @AfterEach
  void tearDown() {
    commentLikeRepository.deleteAll();
    commentRepository.deleteAll();
  }

  @Test
  @DisplayName("서로 다른 사용자 20명이 동시에 좋아요를 눌러도 likeCount 손실 없이 정확히 20이 된다")
  void concurrentLikesFromDistinctUsersPreserveExactCount() throws InterruptedException {
    Comment comment = commentRepository.saveAndFlush(
        Comment.create(UUID.randomUUID(), UUID.randomUUID(), "동시성 테스트 댓글")
    );

    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    List<Throwable> unexpectedErrors = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      UUID userId = UUID.randomUUID();
      executor.submit(() -> {
        readyLatch.countDown();
        try {
          startLatch.await();
          commentService.likeComment(comment.getId(), userId);
          successCount.incrementAndGet();
        } catch (Throwable t) {
          synchronized (unexpectedErrors) {
            unexpectedErrors.add(t);
          }
        } finally {
          doneLatch.countDown();
        }
      });
    }

    readyLatch.await();
    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
    assertThat(unexpectedErrors).isEmpty();
    assertThat(successCount.get()).isEqualTo(threadCount);
    assertThat(updated.getLikeCount()).isEqualTo(threadCount);
  }

  @Test
  @DisplayName("동일 사용자가 동시에 10번 좋아요를 요청해도 1번만 성공하고 likeCount는 1을 유지한다")
  void concurrentDuplicateLikesFromSameUserOnlySucceedOnce() throws InterruptedException {
    Comment comment = commentRepository.saveAndFlush(
        Comment.create(UUID.randomUUID(), UUID.randomUUID(), "중복 좋아요 동시성 테스트")
    );
    UUID sameUserId = UUID.randomUUID();

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger duplicateCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        readyLatch.countDown();
        try {
          startLatch.await();
          commentService.likeComment(comment.getId(), sameUserId);
          successCount.incrementAndGet();
        } catch (DuplicateCommentLikeException ex) {
          duplicateCount.incrementAndGet();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    readyLatch.await();
    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
    assertThat(successCount.get()).isEqualTo(1);
    assertThat(duplicateCount.get()).isEqualTo(threadCount - 1);
    assertThat(updated.getLikeCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("동일 사용자가 동시에 10번 좋아요 취소를 요청해도 1번만 감소하고 likeCount는 0이 된다")
  void concurrentUnlikesFromSameUserOnlyDecrementOnce() throws InterruptedException {
    Comment comment = commentRepository.saveAndFlush(
        Comment.create(UUID.randomUUID(), UUID.randomUUID(), "좋아요 취소 동시성 테스트")
    );
    UUID sameUserId = UUID.randomUUID();
    commentService.likeComment(comment.getId(), sameUserId);

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger notFoundCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        readyLatch.countDown();
        try {
          startLatch.await();
          commentService.unlikeComment(comment.getId(), sameUserId);
          successCount.incrementAndGet();
        } catch (CommentLikeNotFoundException ex) {
          notFoundCount.incrementAndGet();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    readyLatch.await();
    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
    assertThat(successCount.get()).isEqualTo(1);
    assertThat(notFoundCount.get()).isEqualTo(threadCount - 1);
    assertThat(updated.getLikeCount()).isEqualTo(0L);
  }

  @Test
  @DisplayName("논리 삭제 시 좋아요 정보는 유지되고 물리 삭제 시 좋아요 정보까지 모두 삭제된다")
  void softDeletePreservesLikesAndHardDeleteRemovesAllRelatedData() {
    UUID authorId = UUID.randomUUID();
    UUID likerId = UUID.randomUUID();
    Comment comment = commentRepository.saveAndFlush(
        Comment.create(UUID.randomUUID(), authorId, "삭제 정책 검증 댓글")
    );
    commentService.likeComment(comment.getId(), likerId);

    commentService.softDeleteComment(comment.getId());
    assertThat(commentRepository.findById(comment.getId())).isPresent();
    assertThat(commentRepository.findByIdAndDeletedFalse(comment.getId())).isEmpty();
    assertThat(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), likerId)).isTrue();

    commentService.hardDeleteComment(comment.getId());
    assertThat(commentRepository.findById(comment.getId())).isEmpty();
    assertThat(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), likerId)).isFalse();
  }
}
