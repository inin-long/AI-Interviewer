# 文件系统

## 概述

AI Interviewer 使用本地文件系统存储用户数据。所有文件路径通过 `PathService` 抽象，禁止硬编码路径。

---

## 目录结构

```
AI-Interviewer/                      # 应用根目录
├── database/
│   └── app.db                       # SQLite 数据库
├── users/
│   └── {user-id}/                   # 用户隔离目录
│       ├── resumes/                 # 简历文件
│       ├── documents/               # 知识文档
│       ├── reports/                 # 面试报告
│       └── vector/                  # Lucene 向量索引
├── logs/                            # 应用日志
├── temp/                            # 临时文件
└── config/
    └── application-local.yml        # 本地配置
```

---

## 路径规则

### 用户数据隔离

每个用户拥有独立的目录：

```
users/
├── 1/                              # 用户 1
│   ├── resumes/
│   ├── documents/
│   ├── reports/
│   └── vector/
├── 2/                              # 用户 2
│   ├── resumes/
│   ├── documents/
│   ├── reports/
│   └── vector/
└── ...
```

### 文件命名

使用 UUID 前缀避免文件名冲突：

```
UUID_originalName.pdf
```

示例：`a81c2f3b-1234-5678-90ab-cdef01234567_resume.pdf`

### 数据库存储

数据库同时存储：
- `original_name`：原始文件名
- `storage_path`：实际存储路径（含 UUID）

---

## PathService

所有文件路径必须通过 `PathService` 获取，禁止硬编码。

| 方法 | 返回路径 | 说明 |
|------|----------|------|
| `getResumePath(userId)` | `users/{id}/resumes/` | 简历存储目录 |
| `getDocumentPath(userId)` | `users/{id}/documents/` | 知识文档目录 |
| `getReportPath(userId)` | `users/{id}/reports/` | 报告存储目录 |
| `getVectorPath(userId)` | `users/{id}/vector/` | 向量索引目录 |
| `getDatabasePath()` | `database/app.db` | 数据库文件 |
| `getTempPath()` | `temp/` | 临时文件目录 |
| `getLogPath()` | `logs/` | 日志目录 |

### 正确用法

```java
// ✅ 正确
Path resumePath = pathService.getResumePath(userId);

// ❌ 错误
Path resumePath = Path.of("users/" + userId + "/resumes");
```

---

## FileStorageService

文件存储的抽象层。

### 接口定义

| 方法 | 说明 |
|------|------|
| `store(category, userId, fileName, inputStream)` | 存储文件 |
| `retrieve(category, userId, storageName)` | 读取文件 |
| `delete(category, userId, storageName)` | 删除文件 |
| `list(category, userId)` | 列出文件 |

### 存储类别

| 枚举值 | 说明 | 路径 |
|--------|------|------|
| `RESUMES` | 简历文件 | `users/{id}/resumes/` |
| `KNOWLEDGE` | 知识文档 | `users/{id}/documents/` |
| `REPORTS` | 面试报告 | `users/{id}/reports/` |
| `VECTOR` | 向量索引 | `users/{id}/vector/` |

### 实现

`LocalFileStorageService`：

- 本地文件系统存储
- 原子写入（先写临时文件，再重命名）
- 文件名 UUID 前缀
- 目录自动创建

---

## 初始化流程

应用启动时，`ApplicationLauncher` 创建必要的目录结构：

```
1. 解析 AI_INTERVIEWER_HOME 环境变量
2. 创建根目录（如不存在）
3. 创建 database/ 目录
4. 创建 users/ 目录
5. 创建 logs/ 目录
6. 创建 temp/ 目录
7. 创建 config/ 目录
8. 启动 JavaFX 应用
```

---

## 文件大小限制

| 类型 | 最大值 | 说明 |
|------|--------|------|
| 简历文件 | 20MB | ResumeService 强制检查 |
| 知识文档 | 无硬性限制 | 由磁盘空间决定 |
| 临时文件 | - | 定期清理 |

---

## 关键类

| 类 | 位置 | 职责 |
|---|---|---|
| `PathService` | `infrastructure/file/` | 路径抽象接口 |
| `DefaultPathService` | `infrastructure/file/` | 路径实现 |
| `FileStorageService` | `infrastructure/file/` | 文件存储接口 |
| `LocalFileStorageService` | `infrastructure/file/` | 本地文件存储实现 |
| `StoredFile` | `infrastructure/file/` | 存储文件信息 |
| `ApplicationLauncher` | 根包 | 应用启动和目录初始化 |
