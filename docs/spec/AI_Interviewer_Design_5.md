# 第五部分：JavaFX UI 架构、页面定义、组件库、CSS Design System

本部分定义桌面应用 UI 实现规范。

目标：

明确：

- JavaFX 应用结构
- 页面切换机制
- Controller 职责
- UI 组件规范
- CSS 样式体系
- 页面信息架构

------

# 45. JavaFX 总体架构

## 45.1 UI 定位

JavaFX 负责：

- 页面展示
- 用户交互
- 状态展示
- 输入采集

不负责：

- 业务逻辑
- 数据持久化
- AI 调用

------

架构：

```
JavaFX UI

    ↓

Controller

    ↓

Application Service

    ↓

Domain / Infrastructure
```

------

# 46. 主窗口设计

## 46.1 窗口模式

采用：

> 单主窗口 + View 切换模式

------

不采用：

```
❌ 多窗口

❌ MDI窗口

❌ 浏览器Tab模式
```

------

# 46.2 MainWindow

结构：

```
MainWindow


┌────────────────────────────┐
│            Header          │
├────────────┬───────────────┤
│            │               │
│ Navigation │ ContentArea   │
│            │               │
│            │               │
└────────────┴───────────────┘
```

------

对应：

```
ui/view/

└── MainWindow.fxml
```

------

# 47. 页面导航系统

## 47.1 ViewManager

负责：

页面加载和切换。

目录：

```
ui/navigation/

├── ViewManager.java

└── Route.java
```

------

接口：

```
public interface ViewManager {


    void switchView(
        Route route
    );


}
```

------

# 47.2 Route

定义页面：

```
public enum Route {


    DASHBOARD,


    RESUME,


    PROFILE,


    INTERVIEW,


    KNOWLEDGE,


    HISTORY,


    REPORT,


    SETTING

}
```

------

# 48. Controller 规范

## 48.1 Controller 职责

Controller 只负责：

- 初始化页面
- 接收用户事件
- 调用 Service
- 更新 UI

------

不负责：

```
❌ SQL

❌ 文件操作

❌ AI调用

❌ 复杂业务逻辑
```

------

# 48.2 Controller 生命周期

页面打开：

```
FXML Loader

↓

Spring创建Controller

↓

initialize()

↓

加载数据

↓

展示
```

------

示例：

```
@Controller
public class ResumeController {


    @Autowired
    ResumeService resumeService;


    @Override
    public void initialize(){

        loadData();

    }

}
```

------

# 49. 页面定义

------

# 49.1 Dashboard 首页

路径：

```
DashboardView.fxml
```

用途：

应用首页。

展示：

- 最近面试
- 简历状态
- 知识库状态
- 快捷入口

------

布局：

```
┌─────────────────┐
│ 今日概览        │
├─────────────────┤
│ 最近面试        │
├─────────────────┤
│ 快捷操作        │
└─────────────────┘
```

------

# 49.2 ResumeView 简历管理

功能：

- 上传简历
- 查看简历
- 删除简历
- 查看解析状态

------

布局：

```
简历列表


+ 上传简历


Java Backend Resume

状态:
已解析
```

------

# 49.3 ProfileView 候选人画像

展示：

AI 提取的信息：

- 技能
- 项目
- 工作经历

------

支持：

手动修改。

------

# 49.4 InterviewView 面试页面

核心页面。

------

布局：

```
┌───────────────────────┐
│ 当前阶段              │
├───────────────────────┤
│                       │
│ AI消息区域            │
│                       │
├───────────────────────┤
│ 输入区域              │
│                       │
└───────────────────────┘
```

------

功能：

- AI 流式输出
- 用户输入
- 暂停
- 结束面试

------

特殊：

正在面试时离开：

提示：

```
当前面试正在进行

是否离开？
```

------

# 49.5 KnowledgeView 知识库

功能：

- 上传文档
- 查看文档
- 删除文档
- 查看索引状态

------

状态：

```
上传中

解析中

索引中

完成
```

------

# 49.6 HistoryView 历史记录

功能：

- 查看历史面试
- 搜索历史
- 打开报告

------

搜索范围：

支持：

- 面试标题
- 岗位
- 标签
- 日期

------

不支持：

```
❌ 聊天内容全文搜索
```

------

# 49.7 ReportView 报告页面

展示：

Markdown。

------

功能：

- 阅读报告
- 导出预留

------

核心组件：

```
MarkdownView
```

------

# 49.8 SettingView 设置页面

功能：

- LLM 配置检查
- 数据目录查看
- 用户设置

------

不提供：

管理员后台。

------

# 50. UI Component Library

## 50.1 设计目标

建立项目内部组件库。

原因：

保证：

- 样式统一
- 代码复用
- AI 编码一致

------

目录：

```
ui/component/

├── base/

├── common/

└── advanced/
```

------

# 51. 基础组件

------

# 51.1 AppButton

替代：

JavaFX Button。

支持：

类型：

```
PRIMARY

SECONDARY

DANGER
```

------

示例：

```
<AppButton
    text="开始面试"
    type="primary"
/>
```

------

# 51.2 AppCard

统一卡片。

用途：

- 面试卡片
- 简历卡片
- 报告卡片

------

# 51.3 AppDialog

统一弹窗。

用于：

- 确认
- 错误
- 提示

------

# 52. 通用组件

------

## LoadingView

用途：

显示：

- AI生成
- 文档解析
- Embedding

------

## EmptyView

用途：

空状态。

例如：

```
暂无简历

[上传]
```

------

## ErrorView

统一错误展示：

例如：

```
操作失败

AI服务不可用

[重试]
```

------

# 53. 高级组件

------

## MarkdownView

用途：

展示：

- AI回答
- 报告
- 文档

------

支持：

- 标题
- 列表
- 代码块
- 表格

------

## FileUploadView

用途：

统一文件上传。

支持：

- 文件选择
- 拖拽
- 上传状态

------

## CodeEditorView

预留：

用于：

- 技术代码输入
- 算法题

------

# 54. CSS Design System

## 54.1 原则

所有 UI 样式：

统一管理。

------

禁止：

```
button.setStyle()
```

------

# 54.2 CSS目录

```
resources/css/

├── app.css

├── colors.css

├── typography.css

├── components.css

└── views.css
```

------

# 55. CSS 分层

------

## colors.css

定义：

颜色变量。

例如：

```
.root {

    -primary-color:#2563EB;

    -background-color:#F8FAFC;

}
```

------

## typography.css

定义：

字体：

- 标题
- 正文
- 辅助文本

------

## components.css

组件样式：

例如：

```
.primary-button {

}


.card {

}
```

------

## views.css

页面样式：

例如：

```
.interview-view {

}
```

------

# 56. UI 状态管理

采用：

Spring ApplicationState。

------

结构：

```
ApplicationState

├── UserSessionState

├── InterviewRuntimeState

└── UIState
```

------

# 56.1 UserSessionState

保存：

当前用户。

------

# 56.2 InterviewRuntimeState

保存：

当前面试运行状态。

例如：

```
sessionId

stage

running
```

------

# 56.3 UIState

保存：

例如：

```
currentRoute

sidebarState
```

------

# 57. 页面通信

采用：

Spring ApplicationEvent。

------

流程：

```
Service

↓

Publish Event

↓

Controller Listener

↓

refresh()
```

------

示例：

简历解析完成：

```
ResumeParsedEvent

↓

ResumeController.refresh()
```

------

禁止：

```
❌ Controller调用Controller

❌ static全局变量

❌ 页面直接修改其他页面
```
