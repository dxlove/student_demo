package com.demo.studentdemo.config;


import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * StreamBatchProcessor - 基于生产者/消费者的批量处理器 (Spring Boot friendly)
 *
 * 使用：
 *   - 注入 Spring 管理的 ThreadPoolTaskExecutor（bean 名称随意）或不注入则使用默认内置线程池
 *   - 调用 processAndWait(...) 或 processAsync(...)
 */
@Component
public class StreamBatchProcessor {

    // ---------- Builder / config ----------
    public static class Builder {
        private int workerThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        private int queueCapacity = 1000;
        private long perTaskTimeoutMillis = 0L;
        private int maxRetries = 0;
        private Duration producerOfferTimeout = Duration.ofSeconds(1);
        private ThreadPoolTaskExecutor providedExecutor = null;

        public Builder workerThreads(int w) { this.workerThreads = w; return this; }
        public Builder queueCapacity(int q) { this.queueCapacity = q; return this; }
        public Builder perTaskTimeoutMillis(long ms) { this.perTaskTimeoutMillis = ms; return this; }
        public Builder maxRetries(int r) { this.maxRetries = r; return this; }
        public Builder producerOfferTimeout(Duration d) { this.producerOfferTimeout = d; return this; }
        public Builder workerExecutor(ThreadPoolTaskExecutor exec) { this.providedExecutor = exec; return this; }

        public StreamBatchProcessor build() { return new StreamBatchProcessor(this); }
    }

    public static Builder builder() { return new Builder(); }

    // ---------- instance fields ----------
    private final int workerThreads;
    private final int queueCapacity;
    private final long perTaskTimeoutMillis;
    private final int maxRetries;
    private final Duration producerOfferTimeout;

    // executors
    private final ThreadPoolTaskExecutor workerExecutor; // backing worker pool (Spring-friendly)
    private final ScheduledExecutorService scheduler;   // shared scheduler to dispatch interrupts on timeout

    // metrics counters (simple)
    private final AtomicLong produced = new AtomicLong();
    private final AtomicLong consumed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    private final AtomicBoolean internalExecutorOwned; // whether we created workerExecutor (for shutdown)

    // ---------- Constructors ----------
    /**
     * Constructor used when a Spring-managed ThreadPoolTaskExecutor is injected.
     * @param injectedExecutor may be null (then default will be created)
     */
    @Autowired
    public StreamBatchProcessor(@Nullable ThreadPoolTaskExecutor injectedExecutor) {
        // reasonable defaults; user can still use builder() for custom instances
        Builder b = new Builder();
        if (injectedExecutor != null) {
            // use injected executor and align workerThreads with its core pool size if possible
            this.workerExecutor = injectedExecutor;
            this.workerThreads = Math.max(1, injectedExecutor.getCorePoolSize());
            this.internalExecutorOwned = new AtomicBoolean(false);
        } else {
            // create default executor
            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
            int defaultThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
            exec.setCorePoolSize(defaultThreads);
            exec.setMaxPoolSize(defaultThreads);
            exec.setQueueCapacity(0); // we use our own bounded queue, set internal queue small/0 to avoid double-buffering
            exec.setThreadNamePrefix("stream-batch-worker-");
            exec.initialize();

            this.workerExecutor = exec;
            this.workerThreads = defaultThreads;
            this.internalExecutorOwned = new AtomicBoolean(true);
        }

        // default other configs
        this.queueCapacity = Math.max(100, Runtime.getRuntime().availableProcessors() * 256);
        this.perTaskTimeoutMillis = 0L;
        this.maxRetries = 0;
        this.producerOfferTimeout = Duration.ofSeconds(1);

        // single-thread scheduler for timeout interrupts (daemon)
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stream-batch-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // Private builder ctor (for builder.build())
    private StreamBatchProcessor(Builder b) {
        this.workerThreads = Math.max(1, b.workerThreads);
        this.queueCapacity = Math.max(1, b.queueCapacity);
        this.perTaskTimeoutMillis = Math.max(0L, b.perTaskTimeoutMillis);
        this.maxRetries = Math.max(0, b.maxRetries);
        this.producerOfferTimeout = b.producerOfferTimeout == null ? Duration.ofSeconds(1) : b.producerOfferTimeout;

        if (b.providedExecutor != null) {
            this.workerExecutor = b.providedExecutor;
            this.internalExecutorOwned = new AtomicBoolean(false);
        } else {
            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
            exec.setCorePoolSize(this.workerThreads);
            exec.setMaxPoolSize(this.workerThreads);
            exec.setQueueCapacity(0); // we rely on external bounded queue
            exec.setThreadNamePrefix("stream-batch-worker-");
            exec.initialize();
            this.workerExecutor = exec;
            this.internalExecutorOwned = new AtomicBoolean(true);
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stream-batch-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // ---------- Core API ----------
    /**
     * 阻塞地处理整个 iteratorSupplier 提供的数据流，直到所有已提交任务完成或发生不可恢复错误。
     *
     * @param iteratorSupplier  提供数据的 supplier（每次调用应返回新的 iterator）
     * @param mapper            处理函数 T -> R
     * @param resultConsumer    结果消费 (T, R)
     * @param errorConsumer     异常回调 (T, Throwable)；如果 iterator 读取阶段出错，item 为 null
     * @param <T>               输入类型
     * @param <R>               输出类型
     */
    public <T, R> void processAndWait(Supplier<Iterator<T>> iteratorSupplier,
                                      Function<T, R> mapper,
                                      BiConsumer<T, R> resultConsumer,
                                      BiConsumer<T, Throwable> errorConsumer) throws InterruptedException {

        Objects.requireNonNull(iteratorSupplier, "iteratorSupplier");
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(resultConsumer, "resultConsumer");

        final BlockingQueue<T> queue = new ArrayBlockingQueue<>(queueCapacity);
        final AtomicBoolean producerFinished = new AtomicBoolean(false);
        final AtomicBoolean stopRequested = new AtomicBoolean(false);

        // reset counters
        produced.set(0);
        consumed.set(0);
        failed.set(0);

        // start worker threads - these threads are actual consumers and run on workerExecutor
        CountDownLatch consumersDone = new CountDownLatch(workerThreads);
        for (int i = 0; i < workerThreads; i++) {
            workerExecutor.execute(() -> {
                try {
                    while (true) {
                        if (stopRequested.get() && producerFinished.get() && queue.isEmpty()) {
                            break;
                        }
                        T item = null;
                        try {
                            item = queue.poll(200, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            // respect stop request
                            if (stopRequested.get()) break;
                            // else continue
                        }
                        if (item == null) {
                            if (producerFinished.get() && queue.isEmpty()) {
                                break;
                            }
                            continue;
                        }

                        boolean success = false;
                        int attempts = 0;
                        while (!success) {
                            attempts++;
                            ScheduledFuture<?> canceller = null;
                            Thread current = Thread.currentThread();
                            try {
                                // schedule interrupt on this worker thread if timeout configured
                                if (perTaskTimeoutMillis > 0) {
                                    canceller = scheduler.schedule(() -> {
                                        // interrupt the worker thread to try to stop the task
                                        current.interrupt();
                                    }, perTaskTimeoutMillis, TimeUnit.MILLISECONDS);
                                }

                                // execute mapper directly on this worker thread (no extra thread creation)
                                R result = mapper.apply(item);

                                // if we reach here, mapper finished normally
                                if (canceller != null) canceller.cancel(false);
                                // clear any interrupt status set by canceller to avoid affecting next tasks
                                if (Thread.interrupted()) {
                                    // Thread.interrupted() clears the flag
                                }

                                // consume result
                                try {
                                    resultConsumer.accept(item, result);
                                } catch (Throwable rcEx) {
                                    // consumer exception - treat as failed (no retry on consumer)
                                    if (errorConsumer != null) errorConsumer.accept(item, rcEx);
                                }
                                consumed.incrementAndGet();
                                success = true;
                            } catch (Throwable ex) {
                                // If interrupted due to timeout, ex may be some InterruptedException or other
                                // Decide whether to retry
                                if (canceller != null) canceller.cancel(false);
                                // clear interrupt status to avoid leaking to next attempt
                                if (Thread.interrupted()) {
                                    // clears the flag
                                }

                                if (attempts > maxRetries) {
                                    failed.incrementAndGet();
                                    if (errorConsumer != null) errorConsumer.accept(item, ex);
                                    break;
                                }
                                // else will retry
                            }
                        } // retry loop
                    } // consumer loop
                } finally {
                    consumersDone.countDown();
                }
            });
        }

        // producer thread (reads iterator and offers to queue with backpressure)
        Thread producer = new Thread(() -> {
            Iterator<T> it = null;
            try {
                it = iteratorSupplier.get();
                if (it == null) {
                    producerFinished.set(true);
                    return;
                }
                while (!stopRequested.get() && it.hasNext()) {
                    T item = it.next();
                    boolean offered = false;
                    while (!offered && !stopRequested.get()) {
                        try {
                            offered = queue.offer(item, producerOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            if (stopRequested.get()) break;
                        }
                    }
                    if (offered) produced.incrementAndGet();
                    else break; // stop requested
                }
            } catch (Throwable e) {
                if (errorConsumer != null) errorConsumer.accept(null, e);
            } finally {
                producerFinished.set(true);
            }
        }, "stream-batch-producer");

        producer.setDaemon(true);
        producer.start();

        // wait producer done, then wait consumers drained
        producer.join();
        // wait until consumers finish processing all items
        consumersDone.await();
    }

    /**
     * 异步版本
     */
    public <T, R> CompletableFuture<Void> processAsync(Supplier<Iterator<T>> iteratorSupplier,
                                                       Function<T, R> mapper,
                                                       BiConsumer<T, R> resultConsumer,
                                                       BiConsumer<T, Throwable> errorConsumer) {
        return CompletableFuture.runAsync(() -> {
            try {
                processAndWait(iteratorSupplier, mapper, resultConsumer, errorConsumer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }, runnable -> {
            // use a lightweight single-thread executor to run the orchestration, not workerExecutor
            Thread t = new Thread(runnable, "stream-batch-orchestrator");
            t.setDaemon(true);
            t.start();
        });
    }

    // ---------- lifecycle ----------
    @PreDestroy
    public void shutdown() {
        // shutdown scheduler
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignore) {}

        if (internalExecutorOwned.get()) {
            try {
                workerExecutor.shutdown();
            } catch (Throwable ignore) {}
        }
    }

    // ---------- metrics accessors ----------
    public long getProduced() { return produced.get(); }
    public long getConsumed() { return consumed.get(); }
    public long getFailed() { return failed.get(); }
}
