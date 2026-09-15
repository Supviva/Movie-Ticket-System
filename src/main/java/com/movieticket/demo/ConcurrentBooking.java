package com.movieticket.demo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发购票验证：让多个线程同时下单。
 *
 * <p>业务层的下单接口都是 {@code synchronized} 的，这里用线程池把多个请求压到
 * 同一瞬间发出，用来验证「同一个座位不会被卖出两次」。没有加锁的实现在这类
 * 场景下会卖出多张票（超卖），这也是并发编程里最典型的竞态演示。</p>
 */
public final class ConcurrentBooking {

    /** 一次并发抢座的统计结果。 */
    public record Result(int threads, int success, int failed) {

        /** 是否只有一个人抢到——同一座位被多人争抢时，正确实现下必须是 {@code true}。 */
        public boolean singleWinner() {
            return success == 1;
        }
    }

    private ConcurrentBooking() {
    }

    /**
     * 所有人都抢同一批座位（超卖场景）。
     *
     * @param waitMillis 等待线程就位与完成的上限，避免测试挂死
     */
    public static Result run(DemoMovieTicketService service, Collection<String> userIds,
                             String sessionId, Set<String> seats, long waitMillis) {
        Map<String, Set<String>> requests = new LinkedHashMap<>();
        for (String userId : userIds) {
            requests.put(userId, seats);
        }
        return runEach(service, requests, sessionId, waitMillis);
    }

    /**
     * 每个用户买自己指定的座位（互不冲突时应当全部成功）。
     *
     * @param requests   用户 id → 该用户要买的座位
     * @param waitMillis 等待线程就位与完成的上限
     */
    public static Result runEach(DemoMovieTicketService service, Map<String, Set<String>> requests,
                                 String sessionId, long waitMillis) {
        List<Map.Entry<String, Set<String>>> tasks = List.copyOf(requests.entrySet());
        int threads = tasks.size();
        if (threads == 0) {
            return new Result(0, 0, 0);
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        try {
            for (Map.Entry<String, Set<String>> task : tasks) {
                String userId = task.getKey();
                Set<String> seats = task.getValue();
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();                     // 等所有线程就位，再一起放行
                        service.buySeatsNow(userId, sessionId, seats);
                        success.incrementAndGet();
                    } catch (DemoException ex) {
                        failed.incrementAndGet();          // 「座位已售出」等业务失败
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        failed.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await(waitMillis, TimeUnit.MILLISECONDS);
            start.countDown();
            done.await(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        return new Result(threads, success.get(), failed.get());
    }
}
