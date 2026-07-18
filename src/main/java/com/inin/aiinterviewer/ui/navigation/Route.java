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
    SETTING(null, "/fxml/settings-view.fxml", "设置"),
    // 新增模块路由
    QUESTION_BANK(null, "/fxml/question-bank-view.fxml", "题库"),
    QUESTION_EDITOR(null, "/fxml/question-editor-view.fxml", "题目编辑"),
    SKILLS_LIBRARY(null, "/fxml/skills-library-view.fxml", "面试技巧"),
    SKILL_ARTICLE_DETAIL(null, "/fxml/skill-article-detail-view.fxml", "技能文章详情"),
    SKILL_ARTICLE_EDITOR(null, "/fxml/skill-article-editor-view.fxml", "技能文章编辑"),
    CAREER_ASSESSMENT(null, "/fxml/career-assessment-view.fxml", "职业评估"),
    CAREER_PLANNING(null, "/fxml/career-planning-view.fxml", "职业规划"),
    CAREER_PLAN_HISTORY(null, "/fxml/career-plan-history-view.fxml", "规划历史"),
    CAREER_REPORT(null, "/fxml/career-report-view.fxml", "评估报告"),
    CAREER_HISTORY(null, "/fxml/career-history-view.fxml", "评估历史"),
    RESUME_OPTIMIZATION_HISTORY(null, "/fxml/resume-optimization-history-view.fxml", "简历优化记录");

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
