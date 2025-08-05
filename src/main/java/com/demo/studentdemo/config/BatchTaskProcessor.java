package com.demo.studentdemo.config;

import com.demo.studentdemo.vo.BatchProcessResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 通用的批量任务并行处理工具类.
 * 使用Spring Boot 3的虚拟线程和CompletableFuture实现高吞吐量.
 */
@Component
public class BatchTaskProcessor {

    private static final Logger log = LoggerFactory.getLogger(BatchTaskProcessor.class);

    private final TaskExecutor taskExecutor;

    @Autowired
    public BatchTaskProcessor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * 【最终健壮版】使用信号量控制持续流式处理，并增加了对数据供给器异常的容错处理.
     *
     * @param <T>              待处理数据的类型
     * @param <R>              处理成功后返回结果的类型
     * @param batchSupplier    批次数据供给器.
     * @param taskProcessor    单个任务处理器.
     * @param concurrencyLevel 最大并发任务数，即信号量的许可数.
     * @return BatchProcessResult<T, R> 包含成功和失败详情的处理结果.
     */
    public <T, R> BatchProcessResult<T, R> processStream(
            Supplier<List<T>> batchSupplier,
            Function<T, R> taskProcessor,
            int concurrencyLevel) {

        log.info("开始流式批量任务处理，最大并发数: {}", concurrencyLevel);
        final Semaphore semaphore = new Semaphore(concurrencyLevel);
        final BatchProcessResult.Builder<T, R> resultBuilder = BatchProcessResult.builder();
        final List<CompletableFuture<Void>> allFutures = new java.util.concurrent.CopyOnWriteArrayList<>();

        // ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
        // 【修正点 1】将主循环包裹在 try-catch 中，以捕获数据供给器的异常
        // ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
        try {
            while (true) {
                List<T> currentBatch;
                try {
                    currentBatch = batchSupplier.get();
                } catch (Exception e) {
                    log.error("数据供给器 (batchSupplier) 在获取数据时发生严重异常，将终止任务提交。", e);
                    break; // 跳出 while 循环
                }

                if (CollectionUtils.isEmpty(currentBatch)) {
                    log.info("数据供给器返回空，所有批次已提交。");
                    break; // 正常结束
                }

                log.info("拉取到新一批数据，数量: {}。准备提交...", currentBatch.size());

                for (T item : currentBatch) {
                    // 在 acquire() 之前检查线程中断状态，可以更快地响应中断信号
                    if (Thread.currentThread().isInterrupted()) {
                        log.warn("主线程已被中断，停止提交新任务。");
                        throw new InterruptedException("主线程中断");
                    }

                    semaphore.acquire();

                    CompletableFuture<Void> future = CompletableFuture
                            .supplyAsync(() -> taskProcessor.apply(item), taskExecutor)
                            .whenComplete((result, ex) -> {
                                try {
                                    if (ex != null) {
                                        // 【修正点 2】更健壮的异常提取逻辑
                                        Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
                                        resultBuilder.addFailure(item, (Exception) cause);
                                        // 注意：这里日志级别可以是 warn 或 error，取决于业务对单个任务失败的容忍度
                                        log.warn("任务处理失败，任务项: {}. 原因: {}", item, cause.getMessage());
                                    } else {
                                        resultBuilder.addSuccess(result);
                                    }
                                } finally {
                                    semaphore.release();
                                }
                            })
                            .thenRun(() -> {});

                    allFutures.add(future);
                }
            }
        } catch (InterruptedException e) {
            // 这个 catch 块现在捕获的是 for 循环内部抛出的 InterruptedException
            log.warn("任务提交过程被中断。将等待已提交的任务完成。", e);
            Thread.currentThread().interrupt(); // 重新设置中断状态
        } catch (Exception e) {
            // 捕获其他意料之外的异常，例如 allFutures.add 失败等不太可能发生的情况
            log.error("批量处理主循环中发生意外异常。", e);
        }

        log.info("所有任务已提交或提交过程已终止，总数: {}. 等待所有已提交的任务执行完毕...", allFutures.size());
        try {
            // 即使主线程被中断，我们仍然应该等待已开始的任务完成
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("在等待所有任务完成时发生异常。", e);
        }

        log.info("所有已提交的任务处理完毕。");

        BatchProcessResult<T, R> finalResult = resultBuilder.build();
        log.info("批量任务处理全部结束。总任务数: {}, 成功: {}, 失败: {}",
                finalResult.totalTasks(), finalResult.successfulTasks(), finalResult.failedTasks());

        return finalResult;
    }
}