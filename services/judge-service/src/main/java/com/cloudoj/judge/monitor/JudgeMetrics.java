package com.cloudoj.judge.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评测服务监控指标
 * 用于监控系统稳定性和评测准确率
 */
@Slf4j
@Component
public class JudgeMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // 计数器
    private Counter submissionCounter;
    private Counter successCounter;
    private Counter failCounter;
    private Counter compileErrorCounter;
    private Counter runtimeErrorCounter;
    private Counter timeoutCounter;
    private Counter wrongAnswerCounter;
    private Counter acceptedCounter;
    
    // 计时器
    private Timer judgeTimer;
    private Timer compileTimer;
    private Timer executeTimer;
    
    // 实时统计
    private final AtomicInteger activeJudgeTasks = new AtomicInteger(0);
    private final AtomicInteger queuedTasks = new AtomicInteger(0);
    private final AtomicLong totalJudgeTime = new AtomicLong(0);
    private final AtomicLong judgeCount = new AtomicLong(0);
    
    // 容器池状态
    private final ConcurrentHashMap<String, AtomicInteger> containerPoolStatus = new ConcurrentHashMap<>();
    
    public JudgeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        // 提交计数器
        submissionCounter = Counter.builder("judge.submission.total")
                .description("Total number of submissions")
                .register(meterRegistry);
        
        successCounter = Counter.builder("judge.submission.success")
                .description("Number of successful submissions")
                .register(meterRegistry);
        
        failCounter = Counter.builder("judge.submission.fail")
                .description("Number of failed submissions")
                .register(meterRegistry);
        
        // 评测结果计数器
        acceptedCounter = Counter.builder("judge.result")
                .tag("status", "ACCEPTED")
                .description("Number of accepted submissions")
                .register(meterRegistry);
        
        wrongAnswerCounter = Counter.builder("judge.result")
                .tag("status", "WRONG_ANSWER")
                .description("Number of wrong answer submissions")
                .register(meterRegistry);
        
        compileErrorCounter = Counter.builder("judge.result")
                .tag("status", "COMPILE_ERROR")
                .description("Number of compile error submissions")
                .register(meterRegistry);
        
        runtimeErrorCounter = Counter.builder("judge.result")
                .tag("status", "RUNTIME_ERROR")
                .description("Number of runtime error submissions")
                .register(meterRegistry);
        
        timeoutCounter = Counter.builder("judge.result")
                .tag("status", "TIME_LIMIT_EXCEEDED")
                .description("Number of timeout submissions")
                .register(meterRegistry);
        
        // 计时器
        judgeTimer = Timer.builder("judge.execution.time")
                .description("Time taken to judge a submission")
                .register(meterRegistry);
        
        compileTimer = Timer.builder("judge.compile.time")
                .description("Time taken to compile code")
                .register(meterRegistry);
        
        executeTimer = Timer.builder("judge.execute.time")
                .description("Time taken to execute code")
                .register(meterRegistry);
        
        // 实时指标
        Gauge.builder("judge.active.tasks", activeJudgeTasks, AtomicInteger::get)
                .description("Number of active judge tasks")
                .register(meterRegistry);
        
        Gauge.builder("judge.queued.tasks", queuedTasks, AtomicInteger::get)
                .description("Number of queued judge tasks")
                .register(meterRegistry);
        
        Gauge.builder("judge.average.time", this, m -> {
            long count = judgeCount.get();
            return count > 0 ? (double) totalJudgeTime.get() / count : 0;
        }).description("Average judge time in milliseconds")
          .register(meterRegistry);
        
        log.info("评测监控指标初始化完成");
    }
    
    // ==================== 提交统计 ====================
    
    public void recordSubmission() {
        submissionCounter.increment();
    }
    
    public void recordSuccess() {
        successCounter.increment();
    }
    
    public void recordFail() {
        failCounter.increment();
    }
    
    // ==================== 评测结果统计 ====================
    
    public void recordJudgeResult(String status) {
        switch (status) {
            case "ACCEPTED":
                acceptedCounter.increment();
                break;
            case "WRONG_ANSWER":
                wrongAnswerCounter.increment();
                break;
            case "COMPILE_ERROR":
                compileErrorCounter.increment();
                break;
            case "RUNTIME_ERROR":
                runtimeErrorCounter.increment();
                break;
            case "TIME_LIMIT_EXCEEDED":
                timeoutCounter.increment();
                break;
        }
    }
    
    // ==================== 时间统计 ====================
    
    public void recordJudgeTime(long timeMs) {
        judgeTimer.record(timeMs, TimeUnit.MILLISECONDS);
        totalJudgeTime.addAndGet(timeMs);
        judgeCount.incrementAndGet();
    }
    
    public void recordCompileTime(long timeMs) {
        compileTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordExecuteTime(long timeMs) {
        executeTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }
    
    // ==================== 任务状态 ====================
    
    public void taskStarted() {
        activeJudgeTasks.incrementAndGet();
    }
    
    public void taskCompleted() {
        activeJudgeTasks.decrementAndGet();
    }
    
    public void taskQueued() {
        queuedTasks.incrementAndGet();
    }
    
    public void taskDequeued() {
        queuedTasks.decrementAndGet();
    }
    
    // ==================== 容器池状态 ====================
    
    public void updateContainerPoolStatus(String image, int count) {
        containerPoolStatus.computeIfAbsent(image, k -> {
            AtomicInteger gauge = new AtomicInteger(count);
            Gauge.builder("judge.container.pool", gauge, AtomicInteger::get)
                    .tag("image", image)
                    .description("Container pool size for image")
                    .register(meterRegistry);
            return gauge;
        }).set(count);
    }
    
    // ==================== 统计报告 ====================
    
    public String getStatsReport() {
        long total = (long) submissionCounter.count();
        long success = (long) successCounter.count();
        long fail = (long) failCounter.count();
        long accepted = (long) acceptedCounter.count();
        long wrongAnswer = (long) wrongAnswerCounter.count();
        long compileError = (long) compileErrorCounter.count();
        long runtimeError = (long) runtimeErrorCounter.count();
        long timeout = (long) timeoutCounter.count();
        
        double successRate = total > 0 ? (success * 100.0 / total) : 0;
        double avgTime = judgeCount.get() > 0 ? (double) totalJudgeTime.get() / judgeCount.get() : 0;
        
        return String.format(
                "评测统计报告:\n" +
                "  总提交: %d, 成功: %d, 失败: %d, 成功率: %.2f%%\n" +
                "  AC: %d, WA: %d, CE: %d, RE: %d, TLE: %d\n" +
                "  活跃任务: %d, 队列任务: %d\n" +
                "  平均评测时间: %.2f ms",
                total, success, fail, successRate,
                accepted, wrongAnswer, compileError, runtimeError, timeout,
                activeJudgeTasks.get(), queuedTasks.get(),
                avgTime
        );
    }
}
