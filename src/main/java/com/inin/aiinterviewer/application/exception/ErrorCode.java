package com.inin.aiinterviewer.application.exception;

public enum ErrorCode {
    USER_ALREADY_EXISTS("USER_001", "用户名已存在"),
    USER_INVALID_CREDENTIALS("USER_002", "用户名或密码错误"),
    USER_NOT_LOGGED_IN("USER_003", "请先登录"),
    FILE_NOT_FOUND("FILE_001", "文件不存在"),
    FILE_TYPE_NOT_SUPPORTED("FILE_002", "不支持的文件类型"),
    FILE_STORAGE_FAILED("FILE_003", "文件保存失败"),
    PLAN_NOT_FOUND("PLAN_001", "面试方案不存在"),
    DOMAIN_PACK_NOT_FOUND("DOMAIN_001", "岗位知识包不存在或已停用"),
    PROFILE_NOT_CONFIRMED("PROFILE_001", "请选择当前账户下已确认的候选人画像"),
    INTERVIEW_NOT_FOUND("INTERVIEW_001", "面试会话不存在"),
    CHECKPOINT_NOT_FOUND("INTERVIEW_002", "未找到可恢复的面试进度"),
    REPORT_RETRY_REQUIRED("INTERVIEW_003", "最终回答已保存，请重新生成报告"),
    DATA_ACCESS_FAILED("DATA_001", "本地数据访问失败"),
    AI_NOT_CONFIGURED("AI_001", "AI 服务尚未配置"),
    AI_CALL_FAILED("AI_002", "AI 服务调用失败"),
    AI_RESPONSE_INVALID("AI_003", "AI 返回格式无效，请重试"),
    TASK_FAILED("TASK_001", "后台任务执行失败"),
    VALIDATION_FAILED("BUSINESS_000", "请检查输入内容"),
    INVALID_STATE("BUSINESS_001", "当前状态不允许执行此操作"),
    SYSTEM_ERROR("SYSTEM_001", "系统发生未知错误");

    private final String code;
    private final String userMessage;

    ErrorCode(String code, String userMessage) {
        this.code = code;
        this.userMessage = userMessage;
    }

    public String code() {
        return code;
    }

    public String userMessage() {
        return userMessage;
    }
}
