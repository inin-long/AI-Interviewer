# 第四部分：RAG 知识库、文档解析、异步任务系统、AI 调用链

本部分定义 AI Interviewer 的知识增强系统以及后台任务体系。

目标：

明确：

- 用户文档如何进入知识库
- 文档如何解析
- 如何生成向量索引
- Agent 如何检索知识
- 长耗时任务如何执行
- AI 调用如何统一管理

------

# 27. RAG 知识库系统设计

## 27.1 RAG 定位

系统采用：

> 用户私有知识库 + Agent Tool 检索模式

------

知识库用于增强：

- 面试问题生成
- 技术追问
- 回答分析
- 报告生成

------

不用于：

- 替代大模型
- 保存所有聊天记录
- 全局共享知识

------

# 27.2 数据隔离

采用：

用户独立知识库。

结构：

```
User A

└── Vector Index A


User B

└── Vector Index B
```

------

禁止：

```
所有用户共享一个 Vector Store
```

原因：

避免：

- 数据泄露
- 检索污染

------

# 28. RAG 数据流程

完整流程：

```
用户上传文档


↓

DocumentService


↓

FileStorageService


↓

DocumentParser


↓

Text Chunker


↓

EmbeddingService


↓

VectorStore


↓

Document Ready


↓

KnowledgeSearchTool


↓

Agent
```

------

# 29. 文档管理

## 29.1 支持格式

V1：

支持：

```
PDF

DOCX

Markdown

TXT
```

------

不支持：

```
❌ PPT

❌ Excel

❌ 图片OCR
```

------

# 29.2 文档状态

Document 表：

字段：

```
status
```

状态：

```
DocumentStatus {


    UPLOADED,


    PARSING,


    INDEXING,


    READY,


    FAILED

}
```

------

流程：

```
上传

↓

UPLOADED

↓

解析

↓

PARSING

↓

Embedding

↓

INDEXING

↓

READY
```

------

# 30. 文档解析系统

## 30.1 Parser 架构

使用：

Apache Tika。

------

目录：

```
infrastructure/document/

├── DocumentParser.java

├── TikaDocumentParser.java

└── ParsedDocument.java
```

------

# 30.2 DocumentParser 接口

```
public interface DocumentParser {


    ParsedDocument parse(
        Path file
    );

}
```

------

# 30.3 解析结果

```
public class ParsedDocument {


    String title;


    String content;


    String fileType;


}
```

------

# 31. 文档切片 Chunk

## 31.1 设计目标

避免：

一次将整个文档发送给 LLM。

------

采用：

Chunk 分割。

------

流程：

```
Document Text

↓

ChunkSplitter

↓

Multiple Chunks
```

------

# 31.2 Chunk 策略

V1：

固定大小切片。

例如：

```
1000 tokens / chunk
```

------

参数：

配置：

```
rag:

  chunk-size: 1000

  overlap: 100
```

------

# 31.3 Chunk 数据

对应：

document_chunk 表。

保存：

```
document_id

chunk_index

content

token_count

vector_id
```

------

# 32. Embedding 系统

## 32.1 Embedding 流程

```
Chunk

↓

EmbeddingService

↓

Vector

↓

VectorStore
```

------

# 32.2 EmbeddingService

位置：

```
infrastructure/ai/

└── EmbeddingService.java
```

------

职责：

统一调用：

Spring AI Embedding API。

------

接口：

```
public interface EmbeddingService {


    float[] embed(
        String text
    );

}
```

------

# 33. Vector Store

## 33.1 技术选择

采用：

Lucene Vector Store。

------

原因：

- 本地运行
- 无需额外服务
- 与桌面应用匹配

------

目录：

```
users/

└── user-id/

    └── vector/
```

------

# 33.2 Vector 查询

流程：

```
Agent

↓

KnowledgeSearchTool

↓

VectorStore

↓

Similarity Search

↓

Relevant Chunks

↓

LLM
```

------

# 34. KnowledgeSearchTool

## 34.1 职责

提供：

Agent 知识查询能力。

------

输入：

```
{
 "query":

 "Redis缓存一致性"
}
```

------

输出：

```
{

"chunks":[

"Redis缓存更新策略..."

]

}
```

------

# 35. 异步任务系统

## 35.1 为什么需要 Task

以下操作耗时：

- 简历解析
- 文档解析
- Embedding
- 报告生成

不能阻塞 JavaFX。

------

因此：

采用：

> 本地任务队列。

------

# 36. Task 架构

结构：

```
TaskService

        ↓

TaskQueue

        ↓

TaskWorker

        ↓

Execute

        ↓

Update Status
```

------

# 36.1 Task 模块

目录：

```
infrastructure/task/

├── TaskService.java

├── TaskWorker.java

├── TaskExecutor.java

└── TaskType.java
```

------

# 37. Task 生命周期

状态：

```
TaskStatus {


    PENDING,


    RUNNING,


    SUCCESS,


    FAILED

}
```

------

流程：

```
创建任务

↓

PENDING

↓

Worker获取

↓

RUNNING

↓

执行

↓

SUCCESS / FAILED
```

------

# 38. TaskWorker

## 38.1 工作方式

启动：

Spring Boot 启动时。

------

流程：

```
while(true){


    获取Pending Task


    执行


    更新状态


}
```

------

# 38.2 Worker 数量

配置：

```
task:

  worker-count: 2
```

------

------

# 39. RetryService

## 39.1 用途

处理：

临时失败。

例如：

- AI API 超时
- 网络异常
- Embedding失败

------

目录：

```
infrastructure/task/

└── RetryService.java
```

------

# 39.2 重试策略

V1：

固定次数重试。

例如：

```
task:

 retry-count: 3
```

------

流程：

```
Task失败

↓

RetryService

↓

等待

↓

重新执行

↓

超过次数

↓

FAILED
```

------

# 40. AI 调用链设计

## 40.1 AI Service

统一入口：

```
infrastructure/ai/

├── ChatService.java

├── EmbeddingService.java

└── RetryService.java
```

------

业务代码：

禁止：

```
new OpenAIClient()
```

------

必须：

```
Agent

↓

ChatService

↓

Spring AI

↓

LLM Provider
```

------

# 41. ChatService

职责：

封装：

- Chat Completion
- Streaming
- 参数管理

------

接口：

```
public interface ChatService {


    String chat(
        Prompt prompt
    );


    Flux<String> stream(
        Prompt prompt
    );

}
```

------

# 42. AI Provider 配置

采用：

单 Provider 模式。

------

配置：

```
llm:

  base-url: ""

  api-key: ""

  chat-model: ""

  embedding-model: ""
```

------

支持：

OpenAI Compatible API。

------

不支持：

```
❌ Provider列表

❌ 自动路由

❌ 多Key轮询
```

------

# 43. Streaming 输出

## 43.1 使用场景

面试过程中：

AI回答需要实时显示。

------

流程：

```
Agent Node

↓

ChatService.stream()

↓

Token Stream

↓

JavaFX UI

↓

Append Text
```

------

# 43.2 UI线程处理

禁止：

后台线程直接：

```
TextArea.appendText()
```

------

必须：

```
Platform.runLater()
```

------

# 44. AI 调用异常

统一转换：

```
LLM Exception

↓

AIException

↓

GlobalExceptionHandler

↓

UI ErrorView
```
