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
