# 服务 API 概览

## 概述

AI Interviewer 采用分层架构，服务层（Application Layer）提供所有业务能力。以下为核心服务的 API 概览。

> 本页面为内部 API 参考，非 REST API。AI Interviewer 是桌面应用，无 HTTP 接口。

---

## 用户服务 (UserService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `register(username, password, nickname)` | 用户名、密码、昵称 | `UserDto` | 用户注册 |
| `login(username, password)` | 用户名、密码 | `UserDto` | 用户登录验证 |
| `findById(id)` | 用户 ID | `UserDto` | 查询用户 |

---

## 简历服务 (ResumeService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `upload(userId, file)` | 用户 ID、文件 | `ResumeDto` | 上传简历 |
| `list(userId)` | 用户 ID | `List<ResumeDto>` | 简历列表 |
| `delete(id)` | 简历 ID | `void` | 删除简历 |
| `getTextContent(id)` | 简历 ID | `String` | 获取提取的文本 |
| `getStatus(id)` | 简历 ID | `ResumeStatus` | 获取解析状态 |

---

## 候选人画像服务 (CandidateProfileService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `create(resumeId, content)` | 简历 ID、内容 | `CandidateProfileDto` | 创建画像 |
| `confirm(id)` | 画像 ID | `void` | 确认画像 |
| `getByResumeId(resumeId)` | 简历 ID | `CandidateProfileDto` | 获取画像 |
| `list(userId)` | 用户 ID | `List<CandidateProfileDto>` | 画像列表 |

---

## 面试方案服务 (InterviewPlanService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `create(plan)` | 方案 DTO | `InterviewPlanDto` | 创建方案 |
| `update(id, plan)` | 方案 ID、DTO | `InterviewPlanDto` | 更新方案 |
| `delete(id)` | 方案 ID | `void` | 删除方案 |
| `list(userId)` | 用户 ID | `List<InterviewPlanDto>` | 方案列表 |
| `findById(id)` | 方案 ID | `InterviewPlanDto` | 查询方案 |

---

## 面试会话服务 (InterviewSessionService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `create(planId, userId)` | 方案 ID、用户 ID | `InterviewSessionDto` | 创建会话 |
| `pause(sessionId)` | 会话 ID | `void` | 暂停面试 |
| `resume(sessionId)` | 会话 ID | `void` | 恢复面试 |
| `appendMessage(sessionId, role, content)` | 会话 ID、角色、内容 | `void` | 追加消息 |
| `checkpoint(sessionId, state)` | 会话 ID、状态 | `void` | 保存 Checkpoint |
| `transitionStage(sessionId, stage)` | 会话 ID、阶段 | `void` | 阶段转换 |

---

## AI 面试服务 (InterviewAgentService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `startInterview(sessionId)` | 会话 ID | `Flux<String>` | 开始面试（流式） |
| `submitAnswer(sessionId, answer)` | 会话 ID、回答 | `Flux<String>` | 提交回答（流式） |
| `getInitialQuestion(sessionId)` | 会话 ID | `String` | 获取初始问题 |

---

## 知识文档服务 (KnowledgeDocumentService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `upload(userId, file)` | 用户 ID、文件 | `KnowledgeDocumentDto` | 上传文档 |
| `list(userId)` | 用户 ID | `List<KnowledgeDocumentDto>` | 文档列表 |
| `delete(id)` | 文档 ID | `void` | 删除文档 |
| `getStatus(id)` | 文档 ID | `KnowledgeStatus` | 获取处理状态 |

---

## 面试报告服务 (InterviewResultService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getReport(sessionId)` | 会话 ID | `InterviewReportDto` | 获取报告 |
| `getHistory(userId)` | 用户 ID | `List<InterviewReportDto>` | 历史报告 |

---

## 后台任务服务 (BackgroundTaskService)

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `enqueue(type, payload)` | 任务类型、负载 | `BackgroundTaskDto` | 入队任务 |
| `claim()` | - | `BackgroundTaskDto` | 领取任务 |
| `complete(id)` | 任务 ID | `void` | 标记完成 |
| `fail(id, error)` | 任务 ID、错误信息 | `void` | 标记失败 |
| `retry(id)` | 任务 ID | `void` | 重试任务 |
| `list(userId)` | 用户 ID | `List<BackgroundTaskDto>` | 任务列表 |

---

## 配置属性

### LlmProperties

| 属性 | 说明 | 示例 |
|------|------|------|
| `baseUrl` | LLM API 基础 URL | `https://api.deepseek.com` |
| `apiKey` | API 密钥 | `sk-...` |
| `chatModel` | 聊天模型名称 | `deepseek-chat` |
| `embeddingModel` | Embedding 模型 | `bge-m3` |
| `timeout` | 请求超时（秒） | `60` |

### StorageProperties

| 属性 | 说明 |
|------|------|
| `rootPath` | 用户文件存储根目录 |

### TaskProperties

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `workerCount` | 2 | 工作线程数 |
| `retryCount` | 3 | 最大重试次数 |
| `pollInterval` | 3000 | 轮询间隔（ms） |
| `retryDelay` | 5000 | 重试延迟（ms） |

### RagProperties

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `chunkSize` | 1000 | 分块大小（token） |
| `chunkOverlap` | 100 | 分块重叠（token） |

---

## 事件列表

| 事件 | 发布者 | 监听者 |
|------|--------|--------|
| `UserLoggedInEvent` | UserService | TaskNotificationCenter |
| `InterviewTurnCompletedEvent` | InterviewAgentService | - |
| `BackgroundTaskCreatedEvent` | BackgroundTaskService | TaskNotificationCenter |
| `BackgroundTaskCompletedEvent` | BackgroundTaskService | TaskNotificationCenter |
| `BackgroundTaskFailedEvent` | BackgroundTaskService | TaskNotificationCenter |
| `BackgroundTaskRetriedEvent` | BackgroundTaskService | TaskNotificationCenter |

---

## 异常码

| 异常类 | 场景 |
|--------|------|
| `AIException` | LLM 调用失败、响应解析错误 |
| `BusinessException` | 业务规则违反（如重复注册） |
| `DataException` | 数据库操作异常 |
| `ConfigurationException` | 配置缺失或无效 |
| `DocumentParseException` | 文档解析失败 |
| `VectorStoreException` | 向量存储操作失败 |
| `FileStorageException` | 文件读写失败 |
| `TaskExecutionException` | 后台任务执行异常 |
