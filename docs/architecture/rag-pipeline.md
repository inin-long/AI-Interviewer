# RAG 管道

## 概述

RAG（Retrieval-Augmented Generation）管道是 AI Interviewer 的知识增强系统。它允许用户上传技术文档、笔记、项目资料等，在面试时自动检索相关知识，让 AI 生成的问题更贴合用户的背景。

---

## 处理管道

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 文档上传  │───→│ Tika     │───→│ 文本分块  │───→│ Embedding│───→│ Lucene   │
│          │    │ 解析     │    │          │    │ 向量化   │    │ 索引     │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                                                                       │
                                                                       ▼
                                                              ┌──────────────┐
                                                              │ 向量相似度搜索 │
                                                              │ (面试时查询)  │
                                                              └──────────────┘
```

---

## 阶段详解

### 1. 文档上传

**入口**：`KnowledgeDocumentService`

- 用户通过 UI 上传文档
- 文件存储到用户专属目录：`users/{user-id}/documents/`
- 文件命名：`UUID_originalName` 避免冲突
- 创建 `knowledge_document` 记录，状态为 `UPLOADED`
- 入队后台任务 `DOCUMENT_PARSE`

### 2. Tika 解析

**实现**：`TikaDocumentParser`

- 使用 Apache Tika 解析文档内容
- 支持格式：PDF、DOCX、Markdown、TXT、HTML 等
- 输出纯文本内容
- 自动检测文档编码

### 3. 文本分块

**实现**：`DocumentChunker`

| 参数 | 默认值 | 说明 |
|------|--------|------|
| chunkSize | 1000 | 每个分块的最大 token 数 |
| chunkOverlap | 100 | 相邻分块的重叠 token 数 |

**分块策略**：
- 按段落/句子边界切分
- 保持语义完整性
- 重叠确保上下文连续性
- 每个分块记录 `chunkIndex` 和 `tokenCount`

### 4. Embedding 向量化

**实现**：`OpenAiEmbeddingService`

- 调用 OpenAI 兼容的 Embedding API
- 配置通过 `LlmProperties.embeddingModel` 指定
- 每个文本分块生成一个向量
- 向量维度取决于模型（如 bge-m3 为 1024）

### 5. Lucene 索引

**实现**：`LuceneVectorStore`

- 每个用户拥有独立的 Lucene 索引目录
- 路径：`users/{user-id}/vector/`
- 使用 Lucene KNN 向量字段
- 支持文档过滤（按 userId 和 sessionId）

---

## 检索流程

面试时，Agent 通过 `KnowledgeSearchTool` 检索相关知识：

```
Agent 生成查询文本
    ↓
KnowledgeSearchTool.execute()
    ↓
EmbeddingService 生成查询向量
    ↓
VectorStorePort.search()
    ├── 过滤条件：userId = 当前用户
    ├── 向量字段：content_embedding
    ├── 相似度：cosine
    └── Top-K：可配置
    ↓
返回 VectorSearchResult 列表
    ↓
注入到 Agent 状态中
    ↓
Agent 参考知识生成问题
```

---

## 会话范围冻结

当面试会话创建时，知识库的查询范围被冻结：

- 创建会话时记录当前用户的所有知识文档 ID
- 面试过程中只搜索这些文档
- 后续上传的新文档不影响历史会话
- 确保面试的可重复性和一致性

---

## 配置参数

通过 `RagProperties` 配置：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| chunkSize | 1000 | 分块大小（token） |
| chunkOverlap | 100 | 分块重叠（token） |

通过 `LlmProperties` 配置：

| 属性 | 说明 |
|------|------|
| embeddingModel | Embedding 模型名称 |
| baseUrl | API 基础 URL |
| apiKey | API 密钥 |

---

## 关键类

| 类 | 位置 | 职责 |
|---|---|---|
| `KnowledgeDocumentService` | `application/service/` | 文档上传、解析、Embedding、索引 |
| `KnowledgeDocumentTaskService` | `application/service/` | 后台任务入队 |
| `KnowledgeDocumentTaskHandler` | `application/task/` | 执行文档处理逻辑 |
| `TikaDocumentParser` | `infrastructure/document/` | Apache Tika 文档解析 |
| `DocumentChunker` | `infrastructure/document/` | 文本分块 |
| `OpenAiEmbeddingService` | `infrastructure/ai/` | 文本向量化 |
| `LuceneVectorStore` | `infrastructure/vector/` | KNN 向量存储和搜索 |
| `KnowledgeSearchTool` | `agent/tool/` | Agent 工具，检索相关知识 |
| `KnowledgeDocumentMapper` | `infrastructure/database/mapper/` | 数据库操作 |
| `DocumentChunkMapper` | `infrastructure/database/mapper/` | 分块数据操作 |

---

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 文档解析失败 | 任务标记为 FAILED，支持手动重试 |
| Embedding API 调用失败 | 自动重试（配置次数） |
| 索引写入失败 | 事务回滚，任务重试 |
| 搜索无结果 | 返回空列表，Agent 不引用知识 |
| 网络中断 | 优雅降级，不阻塞面试流程 |
