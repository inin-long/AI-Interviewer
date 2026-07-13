package com.inin.aiinterviewer.ui.navigation;

public enum Route {
    LOGIN("/fxml/login.fxml", null, "登录"),
    REGISTER("/fxml/register.fxml", null, "创建账户"),
    DASHBOARD("/fxml/main-window.fxml", "/fxml/dashboard-view.fxml", "首页"),
    PLAN(null, "/fxml/plan-view.fxml", "面试方案"),
    RESUME(null, "/fxml/resume-view.fxml", "简历"),
    PROFILE(null, "/fxml/profile-view.fxml", "候选人画像"),
    INTERVIEW(null, null, "模拟面试"),
    KNOWLEDGE(null, "/fxml/knowledge-view.fxml", "知识库"),
    HISTORY(null, "/fxml/history-view.fxml", "面试记录"),
    REPORT(null, null, "面试报告"),
    TASK(null, "/fxml/task-view.fxml", "任务中心"),
    SETTING(null, "/fxml/settings-view.fxml", "设置");

    private final String fxmlPath;
    private final String contentPath;
    private final String title;

    Route(String fxmlPath, String contentPath, String title) {
        this.fxmlPath = fxmlPath;
        this.contentPath = contentPath;
        this.title = title;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public String title() {
        return title;
    }

    public String contentPath() {
        return contentPath;
    }

    public boolean isImplemented() {
        return fxmlPath != null;
    }
}
