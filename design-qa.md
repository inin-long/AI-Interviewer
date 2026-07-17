# 首页设计 QA

- source visual truth path: `D:\Code\Java\AI-Interviewer\docs\ui-reference\home.png`
- implementation screenshot path: `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-final.png`
- viewport: `1672 × 901` JavaFX 内容区（参考图已去除 40px Windows 标题栏后等尺寸比较）
- state: Mahoo 登录态；3 份简历、12 个知识文档、1 个进行中面试、3 条最近面试、3 个最近方案、82 分最近报告
- render method: Spring Boot + JavaFX Scene 直接加载生产 FXML/CSS，在 JavaFX Application Thread 上完成 CSS/layout 后快照
- primary interactions tested: 首页“开始面试”在无方案状态进入新建方案页；侧栏路由切换；顶部任务入口；FXML 全量加载
- runtime errors checked: 完整 `mvn test` 通过；无 JavaFX FXML 加载异常或首页交互异常

## Full-view comparison evidence

- `C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-full-comparison.png`
- 对比结果：侧栏 232px、顶栏 62px、内容左右 22px 边距、114px 横幅、四列指标卡、61/39 主栏比例、最近面试/方案/快捷操作和底部评估区的尺寸与起止位置均对齐参考图。

## Focused region comparison evidence

- 顶栏、横幅与指标卡：`C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-top-comparison.png`
- 最近记录、快捷操作与评估区：`C:\Users\35975\.codex\visualizations\2026\07\17\019f704b-9f99-7db3-87b7-aa0f8ddce3e9\dashboard-lower-comparison.png`

## Required fidelity surfaces

- Fonts and typography: 使用 Microsoft YaHei UI / Segoe UI 回退；标题、指标数字、卡片标题、正文与辅助文字的字号、字重、行高和换行均与参考层级一致，无截断。
- Spacing and layout rhythm: 参考图同尺寸下无水平/垂直滚动条，无卡片碰撞；右栏快捷操作按参考关系覆盖底部评估卡上缘，列表行与按钮保持稳定对齐。
- Colors and visual tokens: 白色表面、`#F7F8FC` 页面底色、蓝紫主色、绿/橙语义色、浅边框和低强度阴影均映射到统一 CSS token 体系。
- Image quality and asset fidelity: 应用图标、顶部 AI 玻璃插画和底部芯片插画均为本次生成的独立高分辨率 PNG，并已按实际槽位缩放、裁切和淡化；无占位图、CSS 假插画或手写 SVG 替代。
- Copy and content: 固定文案与参考图一致；数量、状态、日期、评分、方案与报告摘要均来自真实服务，空状态使用独立闭环文案。
- Icons: 导航、指标、列表、快捷操作统一使用 Ikonli MaterialDesign2；线宽、尺寸、颜色与选中/悬停状态一致。
- Accessibility and states: 核心动作均为可聚焦 Button；任务入口和评分提供 accessibleText；完成、进行中、失败、空状态与悬停状态均有可见反馈。

## Comparison history

1. Initial capture — `dashboard-implementation.png`
   - Earlier findings: P2 可见滚动条；P2 右侧方案/快捷操作列在跨行网格中垂直居中；P2 Ikonli 选择器未命中导致彩色图标变黑；P2 评分环未形成完整圆环。
   - Fixes: 收紧页面底部节奏；设置右栏顶部对齐；修正 `ikonli-font-icon` 样式与 FXML 多 class 声明；改为数据驱动的 JavaFX Arc 环形评分图。
2. Intermediate captures — `dashboard-implementation-v2.png` / `dashboard-implementation-v3.png`
   - Earlier findings: P2 右栏卡片表面与参考层级不一致；P2 快捷操作卡高度不足；P2 最近面试的日期、状态、分数与操作列错位。
   - Fixes: 恢复白色卡面、边框和阴影；按参考图增高快捷操作卡；使用固定列宽、独立间距和尾部操作图标对齐行内容。
3. Final capture — `dashboard-final.png`
   - Post-fix evidence: 完整和局部并排对比均无剩余 P0/P1/P2 问题；核心内容无溢出、遮挡、异常换行或缺失素材。

## Findings

- No actionable P0/P1/P2 findings remain.

## Follow-up polish

- [P3] 参考图中的 Redis 图标是品牌化 3D 图标，实现采用同语义的 MaterialDesign 数据库图标，以保持统一图标家族和可维护性。
- [P3] 生成的顶部 AI 插画比参考图略清晰、蓝紫饱和度略高，但主体、位置、留白和整体光感一致。

## Implementation checklist

- [x] 参考图同尺寸渲染与全屏对比
- [x] 顶部/底部局部放大对比
- [x] 真实数据、空状态和 82 分报告状态
- [x] 核心 CTA 与侧栏/顶部路由交互
- [x] FXML 加载与完整单元测试
- [x] 生成素材落盘并由生产 FXML 使用

final result: passed
