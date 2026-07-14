# 快速开始

## 什么是 AI Interviewer？

AI Interviewer 是一款**本地运行的 AI 技术面试训练工具**。它帮助你通过模拟面试来系统化提升技术面试能力。

**核心特点**：
- 完全本地运行，数据不上传
- AI 驱动的智能面试流程
- RAG 知识增强，问题贴合你的背景
- 六维评估报告

---

## 30 秒概览

```
注册账号 → 上传简历 → 确认画像 → 创建方案 → 开始面试 → 查看报告
```

---

## 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 必须 |
| 操作系统 | Windows 10/11 x64 | 当前仅支持 Windows |
| Maven | 3.9+（首次生成 Wrapper 时） | 之后使用仓库内的 Wrapper |
| LLM API Key | - | DeepSeek、OpenAI 或兼容服务 |

---

## 快速安装

### 1. 获取代码

```bash
git clone <repository-url>
cd AI-Interviewer
```

### 2. 配置 API Key

创建 `application-local.yml` 或设置环境变量：

```yaml
# src/main/resources/application-local.yml
llm:
  base-url: https://api.deepseek.com
  api-key: sk-your-api-key
  chat-model: deepseek-chat
  embedding-model: bge-m3
```

### 3. 构建并运行

```bash
# 运行测试
.\mvnw.cmd clean test

# 启动应用
.\mvnw.cmd javafx:run
```

---

## 首次使用流程

### Step 1: 注册账号

启动应用后，在登录页面点击「注册」，创建本地账号。

### Step 2: 上传简历

进入「简历」页面，上传你的简历文件（PDF/DOCX/Markdown/TXT）。

系统会自动解析简历内容，生成候选人画像。

### Step 3: 确认画像

在「画像」页面查看 AI 生成的候选人画像，确认信息准确。

### Step 4: 创建面试方案

进入「面试方案」页面，创建新的面试方案：

- 选择关联的简历
- 设置岗位信息
- 选择难度和时长
- 可选：关联知识文档

### Step 5: 开始面试

在面试方案中点击「开始面试」，进入面试工作台：

- AI 面试官会根据你的简历和方案提问
- 你可以输入文字回答
- 支持代码回答
- 可以随时暂停和恢复

### Step 6: 查看报告

面试结束后，系统自动生成评估报告：

- 六个维度的评分
- 详细的技术分析
- 优势和不足
- 改进建议

---

## 下一步

- [安装部署](./installation.md) - 详细的环境配置和部署指南
- [日常使用](./usage.md) - 完整的使用流程和技巧
- [产品概述](../product/) - 了解产品的完整功能
- [技术架构](../architecture/) - 深入了解系统设计
