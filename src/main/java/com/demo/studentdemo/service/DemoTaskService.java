package com.demo.studentdemo.service;


import com.demo.studentdemo.config.BatchTaskProcessor;
import com.demo.studentdemo.vo.BatchProcessResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DemoTaskService {

    private static final Logger log = LoggerFactory.getLogger(DemoTaskService.class);

    @Autowired
    private BatchTaskProcessor processor;

    private final Queue<String> dataSource = new ConcurrentLinkedQueue<>();

    public void runDemo() {
        log.info("正在准备模拟数据...");
        // 增加任务数量以更好地观察流式效果
        dataSource.addAll(IntStream.rangeClosed(1, 2000)
                .mapToObj(i -> "任务-" + i)
                .collect(Collectors.toList()));
        log.info("模拟数据准备完成，总共 {} 个任务。", dataSource.size());

        final int BATCH_SIZE = 100;
        Supplier<List<String>> batchSupplier = () -> {
            return IntStream.range(0, BATCH_SIZE)
                    .mapToObj(i -> dataSource.poll())
                    .takeWhile(item -> item != null)
                    .collect(Collectors.toList());
        };

        Function<String, String> taskProcessor = (task) -> {
            try {
                // 模拟更悬殊的执行时间，每10个任务中有一个是长耗时任务
                long sleepTime;
                if (Integer.parseInt(task.substring(3)) % 10 == 0) {
                    // 长耗时任务 (2s - 3s)
                    sleepTime = ThreadLocalRandom.current().nextLong(2000, 3001);
                } else {
                    // 短耗时任务 (50ms - 200ms)
                    sleepTime = ThreadLocalRandom.current().nextLong(50, 201);
                }
                Thread.sleep(sleepTime);

                if (ThreadLocalRandom.current().nextInt(20) == 0) { // 5% 失败率
                    throw new RuntimeException("模拟业务异常: " + task + " 处理失败!");
                }

                String result = task + " 已在 " + sleepTime + "ms 内成功处理 (Thread: " + Thread.currentThread().toString() + ")";
                // 注意：在虚拟线程下，日志输出会非常密集
                log.info("✅ {}", result);
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("任务被中断", e);
            }
        };

        // 设置最大并发数为500
        final int MAX_CONCURRENCY = 500;

        // 调用优化后的流式处理方法
        BatchProcessResult<String, String> result = processor.processStream(batchSupplier, taskProcessor, MAX_CONCURRENCY);

        log.info("----------- 最终处理报告 -----------");
        log.info("总任务数: {}", result.totalTasks());
        log.info("成功任务数: {}", result.successfulTasks());
        log.info("失败任务数: {}", result.failedTasks());
        if (result.failedTasks() > 0) {
            log.warn("失败详情:");
            result.failedItems().forEach((task, ex) -> log.warn("  - 任务 [{}] 失败，原因: {}", task, ex.getMessage()));
        }
        log.info("------------------------------------");
    }
}
