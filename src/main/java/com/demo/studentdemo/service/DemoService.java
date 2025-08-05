package com.demo.studentdemo.service;

import com.demo.studentdemo.config.StreamBatchProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class DemoService {
    @Autowired
    private StreamBatchProcessor processor;

    public void runExample() throws Exception {
        Supplier<Iterator<Integer>> supplier = () -> new Iterator<Integer>() {
            private int i = 0;
            private final int max = 10_000;
            @Override public boolean hasNext() { return i < max; }
            @Override public Integer next() {
                log.info("从中获取{}", i);
                return i++;
            }
        };

        Function<Integer, String> mapper = (i) -> {
            // 模拟耗时差异：小任务和大任务混合
            try {
                if (i % 1000 == 0) Thread.sleep(200); // 少数慢任务
                else Thread.sleep(10); // 大部分快任务
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "result-" + i;
        };

        BiConsumer<Integer, String> resultConsumer = (item, res) -> {
            // 写 DB / MQ / log 等（注意不要阻塞太久）
            // 推荐把写操作也做批处理或放其它线程池
            log.info("done " + item);
        };

        BiConsumer<Integer, Throwable> errorConsumer = (item, t) -> {
            log.error("error item=" + item + ", ex=" + t.getMessage());
        };

        // 自定义 builder: 依据机器及任务特性调优
        StreamBatchProcessor custom = StreamBatchProcessor.builder()
                .workerThreads(Runtime.getRuntime().availableProcessors() * 2) // 偏IO场景可增大
                .queueCapacity(2000)
                .perTaskTimeoutMillis(30_000)
                .maxRetries(1)
                .build();

        custom.processAndWait(supplier, mapper, resultConsumer, errorConsumer);
    }


    public void runExampleAsync() throws Exception {
        // 模拟一个长任务列表
        Supplier<Iterator<Integer>> supplier = () -> new Iterator<>() {
            int i = 1;
            final int max = 50;

            @Override
            public boolean hasNext() {
                return i <= max;
            }

            @Override
            public Integer next() {
                return i++;
            }
        };

        // 模拟处理逻辑（T -> R）
        Function<Integer, String> mapper = num -> {
            try {
                // 模拟不同耗时的任务
                Thread.sleep(num % 5 * 200L + 100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Timeout-" + num;
            }
            return "Task-" + num + " done on " + Thread.currentThread().getName();
        };

        // 正常结果消费
        BiConsumer<Integer, String> resultConsumer = (input, result) -> {
            log.info("[OK] " + input + " -> " + result);
        };

        // 异常消费
        BiConsumer<Integer, Throwable> errorConsumer = (input, ex) -> {
            log.info("[ERR] " + input + " failed: " + ex);
        };

        // 异步执行
        CompletableFuture<Void> future = processor.processAsync(
                supplier,
                mapper,
                resultConsumer,
                errorConsumer
        );

        // 可以在这里做别的事
        log.info("主线程可以继续干别的活...");

        // 等待完成
        future.join();

        log.info("总任务数={}, 成功={}, 失败={}",
                processor.getProduced(),
                processor.getConsumed(),
                processor.getFailed());

    }

}
