# 设计 QA

## 首页定向收敛

- implementation screenshot: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-simplified.png`
- top comparison: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-followup-top-comparison.png`
- body comparison: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-followup-body-comparison.png`
- result: passed；顶部“本地模式/设置”和首页快捷操作已移除，最近方案卡与最近面试卡等高。

## 登录 / 注册页

- source visual truth:
  - `D:\Code\Java\AI-Interviewer\docs\ui-reference\login.png`
  - `D:\Code\Java\AI-Interviewer\docs\ui-reference\register.png`
- implementation screenshots:
  - `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-login.png`
  - `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-register.png`
- combined comparison inputs:
  - `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-login-comparison.png`
  - `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-register-comparison.png`
- viewport: `1672 × 901` JavaFX scene；参考图去除 40px Windows 标题栏后同尺寸比较
- Browser classification: Browser plugin absent；目标为 JavaFX 桌面界面，使用 JavaFX 原生渲染、截图和交互测试

### Visible fidelity

- 品牌区、主标题、能力说明、主视觉、数据安全说明、表单卡、页脚的横纵基线与参考图一致。
- 登录卡实际渲染尺寸 `582 × 684`；注册卡 `588 × 724`，均由自动化测试锁定。
- 文本框、密码显隐、复选框、主次按钮、分隔线、提示信息、边框圆角和阴影已按参考层级重建。
- 根据前一轮用户指令，参考图右上角“本地模式/设置”未恢复；这是有意偏离。
- 中部主视觉已接入 JavaFX 原生循环动画：上下浮动、轻微呼吸缩放和 ±0.7° 漂移；视图离场时自动暂停。

### Interaction verification

- 登录密码显隐可切换且文本保持同步。
- “记住我”会在登录成功后写入或清理本机偏好。
- 忘记密码入口给出本地账户不可云端找回的说明。
- 注册双密码显隐、协议校验、本地使用说明、登录/注册切换和既有提交逻辑均保留。
- `JavaFxFxmlLoadTest` 新增卡片尺寸、素材加载、密码显隐、协议默认态和动画运行断言。
- 完整 `mvn test` 通过；146+ 测试无失败。

### Known asset follow-up

- 中部主视觉的正式生成版仍依赖可用的 ImageGen 连接；当前生产界面暂用参考图精确裁切 `src/main/resources/images/auth/auth-illustration-fallback.png`，视觉比较通过，但不把它冒充为生成素材。

## 登录 / 注册缩放与留白定向收敛

- user-reported screenshot: `C:\Users\35975\AppData\Local\Temp\codex-clipboard-ffbc3627-fd5e-4d83-bbd9-502f410ee25f.png`
- implementation screenshots:
  - `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-login-responsive.png`
  - `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-register-responsive.png`
- same-frame comparison input: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-register-spacing-comparison.png`
- verification viewport: `1536 × 889` JavaFX logical viewport，对应用户高 DPI 桌面下的可用布局尺寸

### Visible fidelity

- 左侧内容区从 `900 × 700` 收敛为 `760 × 620`，品牌、标题、能力说明和主视觉按同一层级缩放，不再互相挤占。
- 登录卡调整为 `480 × 590`，注册卡调整为 `480 × 620`；输入框、按钮、行距和字号同步收敛，未出现裁切或重叠。
- 主布局改为左右 `72px` 固定安全边距 + 中间弹性间隔；右侧表单卡在 1440 宽视口下仍保留完整 `72px` 页边留白。
- 中间弹性间隔最大限制为 `160px`；窄视口优先保证左右安全边距，宽视口避免左右内容被拉得过散。
- 同画幅对比确认：右侧卡片边缘留白恢复，左侧品牌和主视觉密度显著下降，页脚仍保持居中。

### Verification

- `JavaFxFxmlLoadTest` 新增卡片尺寸、左右安全边距和左右内容间隔断言。
- 登录/注册 FXML 加载、密码显隐、协议默认态和动画运行回归通过。

final result: passed（本轮缩放与页边留白调整）

## 登录 / 注册二次间距收敛

- user-reported screenshot: `C:\Users\35975\AppData\Local\Temp\codex-clipboard-fcef6fc1-2ea9-4f91-a670-d1a31684b99b.png`
- implementation screenshot: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-register-tightened-gap.png`
- combined comparison input: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\auth-security-gap-comparison.png`
- 能力列表实际结束位置为 `456px`，数据安全卡由 `548px` 上移至 `470px`，两者逻辑间距由 `92px` 收敛为 `14px`。
- 应用首次启动场景由 `1672 × 901` 调整为 `1440 × 820`；最小窗口约束维持 `1024 × 720`。
- 自动化测试新增安全卡垂直间距与首次窗口尺寸断言。
- 同区域上下对比确认：红框中的大块空档已消失，数据安全卡未遮挡能力文案，主视觉仍保持完整层级。

final result: passed（本轮垂直间距与启动窗口调整）

## 知识库分类化与文档抽屉（2026-07-18）

- source visual truth: `D:\Code\Java\AI-Interviewer\docs\ui-reference\knowledge.png`
- implementation screenshot: `C:\Users\35975\.codex\visualizations\2026\07\18\019f7443-ad60-7703-91ed-cad90a055040\knowledge-implementation.png`
- drawer screenshot: `C:\Users\35975\.codex\visualizations\2026\07\18\019f7443-ad60-7703-91ed-cad90a055040\knowledge-drawer-implementation.png`
- full-view comparison: `C:\Users\35975\.codex\visualizations\2026\07\18\019f7443-ad60-7703-91ed-cad90a055040\knowledge-comparison.png`
- focused body comparison: `C:\Users\35975\.codex\visualizations\2026\07\18\019f7443-ad60-7703-91ed-cad90a055040\knowledge-focus-comparison.png`
- focused drawer comparison: `C:\Users\35975\.codex\visualizations\2026\07\18\019f7443-ad60-7703-91ed-cad90a055040\knowledge-drawer-comparison.png`
- viewport: `1672 × 901` JavaFX scene；参考图裁去顶部 `40px` Windows 标题栏后按同尺寸比较
- state: 5 个文档、5 个分类，覆盖已就绪/处理中/失败状态；抽屉为已就绪 PDF 的概览状态
- rendered screenshot method: JavaFX 原生 `Parent.snapshot`，场景完成 CSS 与布局计算后抓取
- Browser classification: Browser/IAB 不适用于 JavaFX 桌面应用；本轮使用 JavaFX 原生窗口渲染、截图和交互测试

### Full-view and focused comparison evidence

- 信息架构：保留参考图的标题/说明、上传与新建分类、指标带、左侧分类筛选、状态标签、文档行和右侧详情层级；按需求移除了“已关联方案/未关联文档”和文档级方案关联信息。
- 布局与间距：指标带、`205px` 分类栏、文档工具栏、约 `99px` 文档行和底部汇总与参考图保持同一密度；详情从常驻栏改为 `442px` 右侧 Drawer。
- 字体与层级：继续使用产品现有的 Microsoft YaHei UI / Segoe UI 字体栈；标题、指标数值、列表标题、元数据和状态文字均设置了明确字号与字重，无浏览器默认控件字号。
- 颜色与表面：使用白色内容面、`#E1E5EC` 边框、蓝紫主色、绿色成功、橙色处理中、红色失败；卡片圆角、分隔线、选中边框和抽屉遮罩与参考图的视觉语义一致。
- 图标与文件类型：所有导航、上传、分类、PDF/Word/文本、状态和抽屉图标均来自项目既有 Ikonli Material Design 图标库，没有使用文本符号、占位图或手工 SVG。
- 内容与交互：文档行只显示所属分类，不显示关联方案；单击文档打开抽屉，抽屉包含概览、文档内容和索引详情三个可用标签页。
- 响应与可访问性：保留应用 `1024 × 720` 最小窗口约束；按钮有可访问名称，抽屉可由关闭按钮或遮罩关闭，主列表在窄窗口保留可滚动内容。
- image quality: 目标界面没有需要生成或替换的非图标位图素材；参考中的文件图标由同风格矢量图标库忠实实现。

### Above-the-fold copy diff

- 保留：`知识库`、`上传文档`、`新建分类`、`文档总数`、`已完成索引`、`处理失败`、`分类 / 筛选`、`全部文档`、`搜索文档`、`按更新时间`。
- 按产品需求有意替换：`已关联方案文档数/未关联文档数` 改为 `文档分类/处理中`；说明文字改为“供面试过程按分类检索”。
- 按产品需求有意移除：`关联面试方案`、`我关联的`、文档行的关联方案列，以及详情中的方案关联和推荐方案模块。

### Comparison history

- Pass 1 / P2：五个指标最初表现为五张独立卡片，与参考图“一张总数卡 + 四段连续指标带”不一致。已改为独立总数卡和带竖向分隔的四段指标组；复查通过。
- Pass 1 / P2：文档行初始高度为 `88px`，列表底部留白偏大；Drawer 初始宽度为 `486px`。已将行高收敛至 `99px`，Drawer 收敛至 `442px`；复查后密度和右侧比例通过。
- Pass 1 / P2：分类排序在同秒创建时不稳定。已改为按文档数降序，再按创建时间与名称排序；复查后主要分类顺序与参考层级一致。
- Pass 2：重新抓取同尺寸主界面与抽屉，并将参考和实现置于同一比较输入；字体、间距、颜色、图标、内容和交互表面未发现剩余 P0/P1/P2 问题。

### Primary interactions and verification

- 新建分类后可立即出现在分类列表和上传分类选择器中。
- 上传确认按钮在未选择分类时保持禁用；服务层同时拒绝空白分类，不存在“未分类”回退。
- 状态标签、分类筛选、文档搜索、状态筛选、排序、行操作菜单和删除确认均接入真实本地状态。
- 单击文档行可打开通用 `DrawerPane`；抽屉的遮罩、关闭按钮、滑入/滑出生命周期和三页内容均已验证。
- 面试方案编辑器按分类多选；开始面试时把所选分类下当前已就绪文档写入不可变会话快照。
- JavaFX FXML 加载、分类范围集成测试和迁移测试已通过；无渲染异常。

### Intentional deviations

- 参考图的右侧详情是常驻面板，本实现按明确需求改为点击文档后出现的 Drawer。
- 参考图中的文档级方案关联、推荐方案和应用流程模块不再符合分类关联模型，因此没有保留。
- 主窗口侧栏与顶栏沿用项目现有全局壳层尺寸，避免知识库改造破坏其他页面；知识库内容区在该壳层内按参考图还原。

final result: passed
