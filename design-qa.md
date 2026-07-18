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

### Blocking finding

- [P1] 用户要求中部素材必须生成。内置 ImageGen 已重试三次，均因 `chatgpt.com/backend-api/codex/images/generations` 网络错误失败；当前环境未设置 `OPENAI_API_KEY`，不能在未获用户确认的情况下切换到 CLI 回退。
- 当前生产界面暂用参考图中主视觉的精确裁切 `src/main/resources/images/auth/auth-illustration-fallback.png`，视觉比较通过，但不冒充生成素材。

final result: blocked
