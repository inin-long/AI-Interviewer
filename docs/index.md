---
layout: home

hero:
  name: AI Interviewer
  text: AI 技术面试训练工具
  tagline: 本地运行的 AI 面试助手，帮助你系统化提升技术面试能力
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/
    - theme: alt
      text: 产品概述
      link: /product/

features:
  - title: 简历智能分析
    details: 上传 PDF/DOCX/Markdown/TXT 简历，通过 Tika 提取文本，AI 自动生成候选人画像，包含技能、项目经验和技术栈分析
  - title: AI 模拟面试
    details: 基于 LangGraph4j 状态机驱动的智能面试流程，支持流式输出、追问决策、阶段转换和面试暂停恢复
  - title: RAG 知识增强
    details: 上传知识文档，经 Embedding 后存入 Lucene 向量索引，面试时自动检索相关知识，让 AI 提问更贴合你的背景
  - title: 六维评估报告
    details: 从技术能力、问题解决、项目经验、系统设计、沟通表达、综合评价六个维度进行评分，生成 Markdown 格式详细报告
  - title: 本地数据隔离
    details: 所有数据存储在本地 SQLite 数据库，每个用户独立目录，严格隔离，无需联网，隐私安全
  - title: 后台任务队列
    details: 简历解析、知识文档处理、报告生成等耗时操作在后台异步执行，支持自动重试和重启恢复
---
