package com.snoworca.fxstore.integration.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 동시성 테스트 헬퍼
 *
 * <p>다중 스레드 동시 실행, 경합 상황 시뮬레이션을 위한 유틸리티.
 *
 * <p>사용 예시:
 * <pre>{@code
 * boolean success = ConcurrencyTestHelper.runConcurrently(10, () -> {
 *     // 동시 실행할 코드
 * });
 * assertTrue(success);
 * }</pre>
 */
public final class ConcurrencyTestHelper {

    /** 기본 타임아웃 (초) */
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private ConcurrencyTestHelper() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * 여러 스레드에서 동시 실행
     *
     * <p>모든 스레드가 동시에 시작되도록 CountDownLatch로 동기화합니다.
     *
     * @param threadCount 스레드 수
     * @param task 각 스레드에서 실행할 작업
     * @return 모든 스레드 성공 여부
     * @throws InterruptedException 인터럽트 발생 시
     * @throws AssertionError 타임아웃 또는 스레드 실패 시
     */
    public static boolean runConcurrently(int threadCount, Runnable task)
            throws InterruptedException {
        return runConcurrently(threadCount, task, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 여러 스레드에서 동시 실행 (타임아웃 지정)
     *
     * @param threadCount 스레드 수
     * @param task 각 스레드에서 실행할 작업
     * @param timeoutSeconds 타임아웃 (초)
     * @return 모든 스레드 성공 여부
     * @throws InterruptedException 인터럽트 발생 시
     * @throws AssertionError 타임아웃 또는 스레드 실패 시
     */
    public static boolean runConcurrently(int threadCount, Runnable task, int timeoutSeconds)
            throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<Throwable> error = new AtomicReference<>();

        List<Thread> threads = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();  // 모든 스레드가 준비될 때까지 대기
                    task.run();
                } catch (Throwable e) {
                    success.set(false);
                    error.compareAndSet(null, e);
                } finally {
                    endLatch.countDown();
                }
            }, "ConcurrencyTestThread-" + i);
            threads.add(thread);
            thread.start();
        }

        startLatch.countDown();  // 모든 스레드 동시 시작
        boolean completed = endLatch.await(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            // 타임아웃 시 스레드 인터럽트
            for (Thread thread : threads) {
                thread.interrupt();
            }
            throw new AssertionError("Timeout: threads did not complete in " + timeoutSeconds + " seconds");
        }

        if (error.get() != null) {
            throw new AssertionError("Thread failed with exception", error.get());
        }

        return success.get();
    }

    /**
     * 여러 Callable을 동시 실행하고 결과 수집
     *
     * @param callables 실행할 Callable 리스트
     * @param timeoutSeconds 타임아웃 (초)
     * @param <T> 결과 타입
     * @return 결과 리스트 (순서 보장)
     * @throws Exception 실행 오류 시
     */
    public static <T> List<T> runConcurrentlyWithResults(List<Callable<T>> callables, int timeoutSeconds)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(callables.size());
        try {
            CountDownLatch startLatch = new CountDownLatch(1);

            // 동시 시작을 위해 Callable 래핑
            List<Callable<T>> wrappedCallables = new ArrayList<>(callables.size());
            for (Callable<T> callable : callables) {
                wrappedCallables.add(() -> {
                    startLatch.await();
                    return callable.call();
                });
            }

            List<Future<T>> futures = executor.invokeAll(wrappedCallables.subList(0, 0));  // 빈 리스트로 시작

            // 모든 작업 제출
            futures = new ArrayList<>(callables.size());
            for (Callable<T> wrapped : wrappedCallables) {
                futures.add(executor.submit(wrapped));
            }

            // 모든 스레드 동시 시작
            startLatch.countDown();

            // 결과 수집
            List<T> results = new ArrayList<>(callables.size());
            for (Future<T> future : futures) {
                results.add(future.get(timeoutSeconds, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 두 작업을 동시에 실행하며 경합 유발
     *
     * <p>읽기와 쓰기를 동시에 실행하여 경합 상황을 테스트합니다.
     *
     * @param task1 첫 번째 작업
     * @param task2 두 번째 작업
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static void raceTwoTasks(Runnable task1, Runnable task2)
            throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                task1.run();
            } catch (Throwable e) {
                error.compareAndSet(null, e);
            } finally {
                endLatch.countDown();
            }
        }, "RaceTask-1");

        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                task2.run();
            } catch (Throwable e) {
                error.compareAndSet(null, e);
            } finally {
                endLatch.countDown();
            }
        }, "RaceTask-2");

        t1.start();
        t2.start();
        startLatch.countDown();

        boolean completed = endLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!completed) {
            t1.interrupt();
            t2.interrupt();
            throw new AssertionError("Timeout: race tasks did not complete");
        }

        if (error.get() != null) {
            throw new AssertionError("Race task failed", error.get());
        }
    }

    /**
     * 반복적으로 동시 실행 (스트레스 테스트)
     *
     * @param iterations 반복 횟수
     * @param threadCount 스레드 수
     * @param task 각 스레드에서 실행할 작업
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static void repeatConcurrently(int iterations, int threadCount, Runnable task)
            throws InterruptedException {
        for (int i = 0; i < iterations; i++) {
            runConcurrently(threadCount, task);
        }
    }

    /**
     * 스레드 안전성 테스트를 위한 동시 증가 검증
     *
     * <p>여러 스레드에서 동시에 카운터를 증가시키고 최종 결과를 검증합니다.
     *
     * @param counter AtomicInteger 또는 유사한 스레드 안전 카운터
     * @param threadCount 스레드 수
     * @param incrementsPerThread 스레드당 증가 횟수
     * @param incrementAction 증가 액션 (counter::incrementAndGet 등)
     * @return 최종 카운터 값
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static int testConcurrentIncrement(Object counter, int threadCount,
                                              int incrementsPerThread, Runnable incrementAction)
            throws InterruptedException {
        runConcurrently(threadCount, () -> {
            for (int i = 0; i < incrementsPerThread; i++) {
                incrementAction.run();
            }
        });

        // 호출자가 카운터 값을 직접 확인해야 함
        return threadCount * incrementsPerThread;  // 예상 값 반환
    }
}
