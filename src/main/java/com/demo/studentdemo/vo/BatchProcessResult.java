package com.demo.studentdemo.vo;

import java.util.List;
import java.util.Map;

/**
 * 批量处理结果的封装对象.
 *
 * @param <T> 原始任务项的类型
 * @param <R> 任务成功后返回结果的类型
 */
public record BatchProcessResult<T, R>(
        long totalTasks,          // 总任务数
        long successfulTasks,     // 成功任务数
        long failedTasks,         // 失败任务数
        List<R> successResults,   // 成功结果列表
        Map<T, Exception> failedItems // 失败的任务项及其对应的异常
) {
    /**
     * 静态工厂方法，用于创建一个空的、可变的构建器.
     */
    public static <T, R> Builder<T, R> builder() {
        return new Builder<>();
    }

    /**
     * 用于逐步构建BatchProcessResult的构建器类.
     * 这比直接操作Record的字段更灵活，尤其是在循环中。
     */
    public static class Builder<T, R> {
        private final List<R> successResults = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final Map<T, Exception> failedItems = new java.util.concurrent.ConcurrentHashMap<>();

        public void addSuccess(R result) {
            this.successResults.add(result);
        }

        public void addFailure(T item, Exception ex) {
            this.failedItems.put(item, ex);
        }

        public BatchProcessResult<T, R> build() {
            long successful = successResults.size();
            long failed = failedItems.size();
            return new BatchProcessResult<>(
                    successful + failed,
                    successful,
                    failed,
                    List.copyOf(successResults), // 返回不可变列表
                    Map.copyOf(failedItems)      // 返回不可变Map
            );
        }
    }
}
