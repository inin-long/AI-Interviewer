import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'AI Interviewer',
  description: 'AI 技术面试训练工具 - 文档中心',

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/logo.svg' }],
    ['link', { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' }],
  ],

  themeConfig: {
    logo: '/logo.svg',

    nav: [
      { text: '首页', link: '/' },
      {
        text: '产品设计',
        items: [
          { text: '产品概述', link: '/product/' },
          { text: '核心功能', link: '/product/features' },
          { text: 'UI 设计规范', link: '/product/ui-design' },
          { text: '开发路线图', link: '/product/roadmap' },
        ],
      },
      {
        text: '技术架构',
        items: [
          { text: '架构总览', link: '/architecture/' },
          { text: '分层架构', link: '/architecture/layered-architecture' },
          { text: '数据库设计', link: '/architecture/database' },
          { text: 'Agent 系统', link: '/architecture/agent-system' },
          { text: 'RAG 管道', link: '/architecture/rag-pipeline' },
          { text: '后台任务', link: '/architecture/background-tasks' },
          { text: '文件系统', link: '/architecture/file-system' },
        ],
      },
      {
        text: '使用指南',
        items: [
          { text: '快速开始', link: '/guide/' },
          { text: '安装部署', link: '/guide/installation' },
          { text: '日常使用', link: '/guide/usage' },
        ],
      },
      { text: 'API 概览', link: '/api/' },
      {
        text: '设计规格',
        items: [
          { text: '项目总览', link: '/spec/AI_Interviewer_Design_1' },
          { text: '工程结构', link: '/spec/AI_Interviewer_Design_2' },
          { text: 'Agent 设计', link: '/spec/AI_Interviewer_Design_3' },
          { text: '业务流程', link: '/spec/AI_Interviewer_Design_4' },
          { text: '数据模型', link: '/spec/AI_Interviewer_Design_5' },
          { text: '接口设计', link: '/spec/AI_Interviewer_Design_6' },
          { text: '异常处理', link: '/spec/AI_Interviewer_Design_7' },
          { text: 'UI 规范', link: '/spec/AI_Interviewer_UI' },
        ],
      },
    ],

    sidebar: {
      '/guide/': [
        {
          text: '使用指南',
          items: [
            { text: '快速开始', link: '/guide/' },
            { text: '安装部署', link: '/guide/installation' },
            { text: '日常使用', link: '/guide/usage' },
          ],
        },
      ],
      '/product/': [
        {
          text: '产品设计',
          items: [
            { text: '产品概述', link: '/product/' },
            { text: '核心功能', link: '/product/features' },
            { text: 'UI 设计规范', link: '/product/ui-design' },
            { text: '开发路线图', link: '/product/roadmap' },
          ],
        },
      ],
      '/architecture/': [
        {
          text: '技术架构',
          items: [
            { text: '架构总览', link: '/architecture/' },
            { text: '分层架构', link: '/architecture/layered-architecture' },
            { text: '数据库设计', link: '/architecture/database' },
            { text: 'Agent 系统', link: '/architecture/agent-system' },
            { text: 'RAG 管道', link: '/architecture/rag-pipeline' },
            { text: '后台任务', link: '/architecture/background-tasks' },
            { text: '文件系统', link: '/architecture/file-system' },
          ],
        },
      ],
      '/api/': [
        {
          text: 'API 参考',
          items: [
            { text: '服务 API 概览', link: '/api/' },
          ],
        },
      ],
      '/spec/': [
        {
          text: '设计规格',
          items: [
            { text: '项目总览', link: '/spec/AI_Interviewer_Design_1' },
            { text: '工程结构', link: '/spec/AI_Interviewer_Design_2' },
            { text: 'Agent 设计', link: '/spec/AI_Interviewer_Design_3' },
            { text: '业务流程', link: '/spec/AI_Interviewer_Design_4' },
            { text: '数据模型', link: '/spec/AI_Interviewer_Design_5' },
            { text: '接口设计', link: '/spec/AI_Interviewer_Design_6' },
            { text: '异常处理', link: '/spec/AI_Interviewer_Design_7' },
            { text: 'UI 规范', link: '/spec/AI_Interviewer_UI' },
          ],
        },
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com' },
    ],

    footer: {
      message: 'AI Interviewer 文档',
      copyright: '© 2026 AI Interviewer',
    },

    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索文档', buttonAriaLabel: '搜索' },
          modal: {
            noResultsText: '未找到结果',
            resetButtonTitle: '清除查询条件',
            footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' },
          },
        },
      },
    },

    outline: {
      label: '页面导航',
      level: [2, 3],
    },

    lastUpdated: {
      text: '最后更新',
    },

    docFooter: {
      prev: '上一篇',
      next: '下一篇',
    },
  },

  lastUpdated: true,

  cleanUrls: true,

  markdown: {
    lineNumbers: true,
    toc: { level: [2, 3] },
  },
})
