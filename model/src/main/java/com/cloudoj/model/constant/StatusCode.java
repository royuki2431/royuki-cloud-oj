package com.cloudoj.model.constant;

/**
 * 状态码常量类
 */
public class StatusCode {
    
    // ========== 成功响应 ==========
    public static final int SUCCESS = 200;
    
    // ========== 客户端错误 4xx ==========
    public static final int BAD_REQUEST = 400;          // 请求参数错误
    public static final int UNAUTHORIZED = 401;         // 未授权
    public static final int FORBIDDEN = 403;            // 禁止访问
    public static final int NOT_FOUND = 404;            // 资源不存在
    public static final int METHOD_NOT_ALLOWED = 405;   // 方法不允许
    
    // ========== 服务器错误 5xx ==========
    public static final int INTERNAL_ERROR = 500;       // 服务器内部错误
    public static final int SERVICE_UNAVAILABLE = 503;  // 服务不可用
    
    // ========== 业务错误码 ==========
    // 用户相关 1xxx
    public static final int USER_NOT_FOUND = 1001;      // 用户不存在
    public static final int USER_ALREADY_EXISTS = 1002; // 用户已存在
    public static final int PASSWORD_ERROR = 1003;      // 密码错误
    public static final int USER_DISABLED = 1004;       // 用户被禁用
    public static final int TOKEN_EXPIRED = 1005;       // Token过期
    public static final int TOKEN_INVALID = 1006;       // Token无效
    public static final int PERMISSION_DENIED = 1007;   // 权限不足
    
    // 题目相关 2xxx
    public static final int PROBLEM_NOT_FOUND = 2001;   // 题目不存在
    public static final int TEST_CASE_ERROR = 2002;     // 测试用例错误
    
    // 评测相关 3xxx
    public static final int SUBMIT_TOO_FAST = 3001;     // 提交过快
    public static final int LANGUAGE_NOT_SUPPORT = 3002; // 语言不支持
    public static final int CODE_TOO_LONG = 3003;       // 代码过长
    public static final int JUDGE_ERROR = 3004;         // 评测系统错误
    
    // 课程相关 4xxx
    public static final int COURSE_NOT_FOUND = 4001;    // 课程不存在
    public static final int CLASS_NOT_FOUND = 4002;     // 班级不存在
    public static final int HOMEWORK_NOT_FOUND = 4003;  // 作业不存在
    public static final int INVITE_CODE_ERROR = 4004;   // 邀请码错误
    public static final int CLASS_FULL = 4005;          // 班级已满
}
