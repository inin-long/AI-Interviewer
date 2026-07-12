# AI Interviewer

## 本地 AI 技术面试助手

## 系统设计与开发规范文档

版本：V1.0
 文档类型：系统设计说明书
 目标读者：AI 编码 Agent / 开发人员

------

# 第一部分：项目概述、产品边界、技术选型、总体架构

------

# 1. 项目概述

## 1.1 项目名称

AI Interviewer

------

## 1.2 项目定位

AI Interviewer 是一个运行在本地计算机上的 AI 技术面试助手。

它通过 AI Agent 模拟真实技术面试流程，对用户进行：

- 技术能力评估
- 项目经验深挖
- 技术追问
- 面试评分
- 面试报告生成

系统目标不是替代真实面试官，而是提供一个可重复训练、可量化分析的个人技术面试训练工具。

------

## 1.3 核心能力

系统提供以下核心能力：

### 1. 简历分析

用户上传简历：

支持：

- PDF
- DOCX
- Markdown
- TXT

系统：

```
文件上传

↓

文本解析

↓

AI 分析

↓

生成 CandidateProfile

↓

用于后续面试
```

------

### 2. AI 技术面试

系统根据：

- 用户简历
- 岗位要求
- 面试规则

自动生成面试流程。

支持：

- AI 提问
- 用户回答
- AI 追问
- 阶段切换
- 面试总结

------

### 3. RAG 知识辅助

用户可以上传：

- 技术文档
- 学习笔记
- 项目资料

系统：

```
文档

↓

文本切片

↓

Embedding

↓

向量索引

↓

Agent 查询
```

用于辅助 AI 生成更加符合用户背景的问题。

------

### 4. 面试评分

系统固定评分维度：

- 技术能力
- 问题解决能力
- 项目经验
- 系统设计能力
- 沟通能力
- 综合评价

AI 根据面试过程动态评分。

------

### 5. 面试报告

生成 Markdown 格式报告：

包含：

- 综合评分
- 技术能力分析
- 优势
- 不足
- 改进建议
- 学习方向

------

# 2. 产品定位

## 2.1 产品类型

本项目属于：

> 本地运行的 AI Agent 桌面应用。

不是：

- SaaS 平台
- 在线招聘系统
- 企业面试管理系统

------

## 2.2 运行模式

采用：

```
单机运行
+
本地多用户
+
本地数据存储
```

结构：

```
用户

↓

JavaFX Desktop Application

↓

Spring Boot Runtime

↓

SQLite

↓

本地文件系统
```

------

## 2.3 用户模型

采用：

本地多用户。

用户：

拥有：

- 独立账号
- 独立简历
- 独立 CandidateProfile
- 独立知识库
- 独立面试记录
- 独立报告

------

数据隔离：

必须保证：

```
User A

不能访问

User B

的数据
```

------

# 3. MVP 功能范围

## 3.1 必须实现功能

------

## 用户系统

支持：

- 用户注册
- 用户登录
- 用户退出

密码：

使用 BCrypt 加密。

------

## 简历管理

支持：

- 上传简历
- 删除简历
- 查看简历
- 简历解析
- 生成 CandidateProfile

------

## CandidateProfile

用于保存：

AI 从简历中提取的信息：

例如：

```
{
 "skills":[
   "Java",
   "Spring Boot",
   "Redis"
 ],

 "projects":[
   {
     "name":"订单系统",
     "technology":[
       "Spring",
       "MySQL"
     ]
   }
 ]
}
```

------

## 面试系统

支持：

- 创建面试
- 开始面试
- AI 提问
- 用户回答
- AI 追问
- 面试暂停
- 面试恢复

------

## Agent 系统

支持：

- LangGraph4j 状态机
- Agent Node
- Tool 调用
- State Checkpoint

------

## 知识库

支持：

- 上传文档
- 文档解析
- Embedding
- 向量检索

------

## 报告系统

支持：

- 自动生成报告
- Markdown 展示
- 历史查看

------

# 4. 明确不实现内容

以下功能不属于 V1：

------

## 云服务相关

不实现：

```
❌ 云同步

❌ 在线账号

❌ SaaS后台

❌ 多设备同步
```

------

## 用户系统扩展

不实现：

```
❌ OAuth登录

❌ 邮箱验证

❌ 找回密码

❌ RBAC权限系统
```

------

## AI 扩展

不实现：

```
❌ 多 Agent

❌ Agent 自由生成流程

❌ Prompt 编辑器

❌ Prompt 市场

❌ 本地大模型运行

❌ 自动模型路由
```

------

## 输入扩展

不实现：

```
❌ 语音输入

❌ OCR

❌ 图片理解
```

------

## 工程扩展

不实现：

```
❌ 自动更新

❌ 系统托盘

❌ 后台常驻服务

❌ CI/CD
```

------

# 5. 技术选型

------

## 5.1 总体技术栈

| 模块       | 技术                |
| ---------- | ------------------- |
| 语言       | Java                |
| 桌面 UI    | JavaFX              |
| 应用框架   | Spring Boot         |
| 数据库     | SQLite              |
| ORM        | MyBatis             |
| 数据库迁移 | Flyway              |
| AI 框架    | Spring AI           |
| Agent 框架 | LangGraph4j         |
| 文档解析   | Apache Tika         |
| 向量存储   | Lucene Vector Store |
| 日志       | SLF4J + Logback     |
| 测试       | JUnit5 + Mockito    |
| 构建       | Maven               |

------

## 5.2 为什么采用 JavaFX

原因：

- 满足 Java 技术栈要求
- 原生桌面应用体验
- 支持 FXML
- 支持 CSS
- 与 Spring Boot 集成成熟

------

## 5.3 为什么采用 Spring Boot

Spring Boot 负责：

- Bean 管理
- Service 管理
- 配置管理
- 数据访问
- 生命周期管理

JavaFX 只负责：

- UI
- 用户交互

------

## 5.4 为什么采用 SQLite

原因：

项目定位：

本地桌面应用。

SQLite：

优点：

- 无需安装数据库服务
- 单文件存储
- 易备份
- 适合本地应用

------

## 5.5 为什么采用 LangGraph4j

Agent 需要：

- 状态管理
- 节点执行
- 流程控制
- Checkpoint

LangGraph 模型符合：

```
State

↓

Node

↓

Decision

↓

Next Node
```

------

# 6. 总体架构

系统采用：

## Spring Boot + JavaFX 单进程架构

------

整体结构：

```
┌───────────────────────────┐
│        JavaFX UI          │
│                           │
│ FXML                      │
│ Controller                │
│ Component Library         │
└──────────────┬────────────┘
               │
               ↓

┌───────────────────────────┐
│    Spring Boot Context    │
│                           │
│ Application Service       │
│ Domain Logic              │
│ Agent Service             │
│ Config                    │
└──────────────┬────────────┘
               │
               ↓

┌───────────────────────────┐
│ Infrastructure Layer      │
│                           │
│ SQLite                    │
│ File System               │
│ Spring AI                 │
│ Lucene                    │
│ Tika                      │
└───────────────────────────┘
```

------

# 7. 核心设计原则

## 7.1 简洁优先

避免：

- 过度工程化
- 微服务拆分
- 不必要抽象

------

## 7.2 分层明确

依赖方向：

```
UI

↓

Application

↓

Domain

↓

Infrastructure
```

禁止：

```
UI

↓

Database
```

------

## 7.3 Agent 可控

AI 不能：

- 修改系统规则
- 修改面试流程
- 创建非法状态

必须经过：

```
Agent Decision

↓

Validator

↓

Execute
```

------

## 7.4 数据本地化

所有用户数据：

存储于：

```
AI-Interviewer/
```

目录。

