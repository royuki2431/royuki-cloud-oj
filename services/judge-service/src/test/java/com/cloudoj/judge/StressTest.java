package com.cloudoj.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 压力测试 - 模拟100人同时在线提交代码评测
 * 
 * 测试目标：
 * 1. 系统在100人同时在线场景下稳定运行
 * 2. 代码评测准确率 >= 99%
 * 3. 无数据丢失或服务崩溃问题
 */
public class StressTest {
    
    // 配置参数
    private static final String BASE_URL = "http://localhost:8083";  // judge-service 地址
    private static final String GATEWAY_URL = "http://localhost:9000"; // API Gateway 地址
    private static final int CONCURRENT_USERS = 100;  // 并发用户数
    private static final int SUBMISSIONS_PER_USER = 5; // 每用户提交次数
    private static final int TOTAL_SUBMISSIONS = CONCURRENT_USERS * SUBMISSIONS_PER_USER;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    
    // 统计数据
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);
    private static final AtomicInteger correctJudgeCount = new AtomicInteger(0);
    private static final AtomicInteger wrongJudgeCount = new AtomicInteger(0);
    private static final AtomicLong totalResponseTime = new AtomicLong(0);
    private static final ConcurrentHashMap<Long, SubmissionResult> submissionResults = new ConcurrentHashMap<>();
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 测试用例 - 包含正确和错误的代码
    private static final List<TestCase> TEST_CASES = List.of(
            // 正确的 Java 代码 - 应该返回 ACCEPTED
            new TestCase("JAVA", 
                    "public class Solution {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        java.util.Scanner sc = new java.util.Scanner(System.in);\n" +
                    "        int[] nums = java.util.Arrays.stream(sc.nextLine().split(\" \")).mapToInt(Integer::parseInt).toArray();\n" +
                    "        int target = sc.nextInt();\n" +
                    "        for (int i = 0; i < nums.length; i++) {\n" +
                    "            for (int j = i + 1; j < nums.length; j++) {\n" +
                    "                if (nums[i] + nums[j] == target) {\n" +
                    "                    System.out.print(i + \" \" + j);\n" +
                    "                    return;\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}", 
                    "ACCEPTED"),
            
            // 错误答案的 Java 代码 - 应该返回 WRONG_ANSWER
            new TestCase("JAVA",
                    "public class Solution {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.print(\"0 0\");\n" +
                    "    }\n" +
                    "}",
                    "WRONG_ANSWER"),
            
            // 编译错误的 Java 代码 - 应该返回 COMPILE_ERROR
            new TestCase("JAVA",
                    "public class Solution {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.print(\"hello\"\n" +  // 缺少分号和括号
                    "    }\n" +
                    "}",
                    "COMPILE_ERROR"),
            
            // 正确的 Python 代码
            new TestCase("PYTHON",
                    "nums = list(map(int, input().split()))\n" +
                    "target = int(input())\n" +
                    "for i in range(len(nums)):\n" +
                    "    for j in range(i+1, len(nums)):\n" +
                    "        if nums[i] + nums[j] == target:\n" +
                    "            print(i, j, end='')\n" +
                    "            exit()\n",
                    "ACCEPTED"),
            
            // 错误的 Python 代码
            new TestCase("PYTHON",
                    "print('wrong answer')",
                    "WRONG_ANSWER")
    );
    
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("       OJ 系统压力测试开始");
        System.out.println("========================================");
        System.out.println("并发用户数: " + CONCURRENT_USERS);
        System.out.println("每用户提交次数: " + SUBMISSIONS_PER_USER);
        System.out.println("总提交数: " + TOTAL_SUBMISSIONS);
        System.out.println("========================================\n");
        
        // 1. 健康检查
        if (!healthCheck()) {
            System.err.println("服务健康检查失败，请确保服务已启动！");
            return;
        }
        
        // 2. 执行压力测试
        long startTime = System.currentTimeMillis();
        runStressTest();
        long endTime = System.currentTimeMillis();
        
        // 3. 等待所有评测完成
        System.out.println("\n等待所有评测完成...");
        Thread.sleep(10000); // 等待10秒让评测完成
        
        // 4. 验证评测结果
        verifyResults();
        
        // 5. 输出测试报告
        printReport(endTime - startTime);
    }
    
    /**
     * 健康检查
     */
    private static boolean healthCheck() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/actuator/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("健康检查: " + response.body());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("健康检查失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行压力测试
     */
    private static void runStressTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(TOTAL_SUBMISSIONS);
        
        System.out.println("开始提交评测任务...\n");
        
        for (int user = 0; user < CONCURRENT_USERS; user++) {
            final int userId = user + 1;
            executor.submit(() -> {
                for (int i = 0; i < SUBMISSIONS_PER_USER; i++) {
                    try {
                        // 随机选择测试用例
                        TestCase testCase = TEST_CASES.get(ThreadLocalRandom.current().nextInt(TEST_CASES.size()));
                        submitCode(userId, testCase);
                    } catch (Exception e) {
                        System.err.println("用户 " + userId + " 提交失败: " + e.getMessage());
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                    
                    // 模拟用户思考时间
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                    } catch (InterruptedException ignored) {}
                }
            });
        }
        
        // 等待所有提交完成
        latch.await(5, TimeUnit.MINUTES);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        System.out.println("\n所有提交任务已发送完成！");
    }
    
    /**
     * 提交代码
     */
    private static void submitCode(int userId, TestCase testCase) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // 构建请求体
        Map<String, Object> requestBody = Map.of(
                "problemId", 1L,
                "userId", (long) userId,
                "language", testCase.language,
                "code", testCase.code
        );
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/judge/submit"))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long responseTime = System.currentTimeMillis() - startTime;
        totalResponseTime.addAndGet(responseTime);
        
        if (response.statusCode() == 200) {
            successCount.incrementAndGet();
            
            // 解析响应获取 submissionId
            JsonNode jsonNode = objectMapper.readTree(response.body());
            if (jsonNode.has("data") && jsonNode.get("data").has("submissionId")) {
                long submissionId = jsonNode.get("data").get("submissionId").asLong();
                submissionResults.put(submissionId, new SubmissionResult(submissionId, testCase.expectedStatus, null));
                System.out.printf("[用户%03d] 提交成功 submissionId=%d, 语言=%s, 预期=%s, 耗时=%dms%n",
                        userId, submissionId, testCase.language, testCase.expectedStatus, responseTime);
            }
        } else {
            failCount.incrementAndGet();
            System.err.printf("[用户%03d] 提交失败 status=%d, body=%s%n", userId, response.statusCode(), response.body());
        }
    }
    
    /**
     * 验证评测结果
     */
    private static void verifyResults() {
        System.out.println("\n========================================");
        System.out.println("       验证评测结果");
        System.out.println("========================================\n");
        
        int verified = 0;
        int pending = 0;
        
        for (Map.Entry<Long, SubmissionResult> entry : submissionResults.entrySet()) {
            Long submissionId = entry.getKey();
            SubmissionResult result = entry.getValue();
            
            try {
                // 查询评测结果
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/judge/result/" + submissionId))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode jsonNode = objectMapper.readTree(response.body());
                    if (jsonNode.has("data") && jsonNode.get("data").has("status")) {
                        String actualStatus = jsonNode.get("data").get("status").asText();
                        result.actualStatus = actualStatus;
                        
                        if ("PENDING".equals(actualStatus) || "JUDGING".equals(actualStatus)) {
                            pending++;
                        } else {
                            verified++;
                            if (result.expectedStatus.equals(actualStatus)) {
                                correctJudgeCount.incrementAndGet();
                            } else {
                                wrongJudgeCount.incrementAndGet();
                                System.err.printf("评测结果不匹配: submissionId=%d, 预期=%s, 实际=%s%n",
                                        submissionId, result.expectedStatus, actualStatus);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("查询结果失败: submissionId=" + submissionId + ", error=" + e.getMessage());
            }
        }
        
        System.out.printf("已验证: %d, 待处理: %d%n", verified, pending);
    }
    
    /**
     * 输出测试报告
     */
    private static void printReport(long totalTime) {
        int total = successCount.get() + failCount.get();
        double successRate = total > 0 ? (successCount.get() * 100.0 / total) : 0;
        double avgResponseTime = successCount.get() > 0 ? (totalResponseTime.get() * 1.0 / successCount.get()) : 0;
        
        int judgeTotal = correctJudgeCount.get() + wrongJudgeCount.get();
        double judgeAccuracy = judgeTotal > 0 ? (correctJudgeCount.get() * 100.0 / judgeTotal) : 0;
        
        System.out.println("\n========================================");
        System.out.println("       压力测试报告");
        System.out.println("========================================");
        System.out.println();
        System.out.println("【提交统计】");
        System.out.printf("  总提交数: %d%n", total);
        System.out.printf("  成功提交: %d%n", successCount.get());
        System.out.printf("  失败提交: %d%n", failCount.get());
        System.out.printf("  提交成功率: %.2f%%%n", successRate);
        System.out.println();
        System.out.println("【评测统计】");
        System.out.printf("  已验证评测: %d%n", judgeTotal);
        System.out.printf("  评测正确: %d%n", correctJudgeCount.get());
        System.out.printf("  评测错误: %d%n", wrongJudgeCount.get());
        System.out.printf("  评测准确率: %.2f%%%n", judgeAccuracy);
        System.out.println();
        System.out.println("【性能统计】");
        System.out.printf("  总耗时: %d ms%n", totalTime);
        System.out.printf("  平均响应时间: %.2f ms%n", avgResponseTime);
        System.out.printf("  吞吐量: %.2f 请求/秒%n", total * 1000.0 / totalTime);
        System.out.println();
        System.out.println("【测试结论】");
        
        boolean passed = true;
        if (successRate < 99) {
            System.out.println("  ❌ 提交成功率低于99%");
            passed = false;
        } else {
            System.out.println("  ✅ 提交成功率达标 (>= 99%)");
        }
        
        if (judgeAccuracy < 99) {
            System.out.println("  ❌ 评测准确率低于99%");
            passed = false;
        } else {
            System.out.println("  ✅ 评测准确率达标 (>= 99%)");
        }
        
        if (failCount.get() > 0) {
            System.out.println("  ⚠️ 存在失败的提交，需要检查");
        }
        
        System.out.println();
        System.out.println("========================================");
        System.out.println(passed ? "       测试通过 ✅" : "       测试失败 ❌");
        System.out.println("========================================");
    }
    
    /**
     * 测试用例
     */
    static class TestCase {
        String language;
        String code;
        String expectedStatus;
        
        TestCase(String language, String code, String expectedStatus) {
            this.language = language;
            this.code = code;
            this.expectedStatus = expectedStatus;
        }
    }
    
    /**
     * 提交结果
     */
    static class SubmissionResult {
        long submissionId;
        String expectedStatus;
        String actualStatus;
        
        SubmissionResult(long submissionId, String expectedStatus, String actualStatus) {
            this.submissionId = submissionId;
            this.expectedStatus = expectedStatus;
            this.actualStatus = actualStatus;
        }
    }
}
