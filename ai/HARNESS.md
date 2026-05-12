# BK-CI AI Harness 实践

本文定义 BK-CI 在 Cursor、CodeBuddy 等 AI 编辑器中的 Harness 组织方式。

这里的 Harness 不是单一提示词，而是 `规则 + 技能 + 工具 + 评测 + 反馈` 组成的工程支架。目标是让
AI 编码过程更稳定、可复盘，并能随着真实失败案例持续改进。

## 分层职责

### Rules

`ai/rules/` 放 always-on 的硬约束，只保留稳定、高频、低噪声的内容。

适合进入 rule：

- 修改代码前必须先读相关 skill
- OpenSpec 变更必须先同步文档
- 跨语言通用的代码红线，例如行长上限
- 仓库级工作流约束，例如新增文件暂存规则

不适合进入 rule：

- 模块百科和背景材料
- 低频排障步骤
- 长示例、payload、状态枚举
- 只适用于单个模块或单次需求的判断

### Skills

`ai/skills/` 是动态上下文层。每个 skill 只解决一类模块或工作流问题，负责告诉 AI：

- 何时应该使用这个 skill
- 何时不应该使用这个 skill
- 修改前先看哪些关键边界
- 常见误判和风险在哪里
- 需要更细信息时读哪些 `reference/`

主 `SKILL.md` 应保持短而可路由，重内容下沉到 `reference/`。

默认读取预算应尽量收紧：

- 默认先读 `1` 个模块主 skill
- 只有任务确实涉及通用分层、语言规范、测试或 API 设计时，最多再补 `1` 个横切 skill
- `00-bkci-global-architecture` 只在模块归属不清、跨模块或需要链路判断时读取
- `reference/` 只在主 skill 不足且有明确理由时读取
- 不要因为“保险起见”持续扩展 skill 链路

### References

`reference/` 用来承载详细资料，例如模块链路、目录说明、长案例、payload、状态表和排障细节。

AI 只有在主 skill 不足以完成任务时才继续读取 reference，避免上下文窗口被低频信息占满。

### Editor Adapters

`.cursor/`、`.codebuddy/` 等编辑器目录是适配层，不是知识权威来源。

治理原则：

- `ai/` 是规则和 skill 的权威源
- 编辑器目录只承载工具要求的格式和薄适配
- 同名 skill 不应在不同编辑器目录里长期分叉
- 如果发现编辑器目录和 `ai/` 不一致，优先修正 `ai/`，再同步适配目录

### Evals

`ai/evals/` 用真实 BK-CI 任务验证 Harness 是否有效。

评测任务不追求覆盖所有场景，优先覆盖 AI 容易做错、返工成本高、跨模块判断强的场景。

## 标准工作流

1. 识别任务类型，优先匹配 rule 和 skill。
2. 复杂或跨模块需求先进入 Plan Mode，调研后再实现。
3. 如果已有 OpenSpec 变更目录，先更新 OpenSpec 文档。
4. 修改代码前按读取预算先读目标模块 skill；只有主 skill 不足时，再升级到全局 skill 或
   `reference/`。
5. 实现后运行最小必要验证，例如 lint、单测、类型检查或人工验收清单。
6. 如果 AI 失败，记录失败原因，并反哺到 rule、skill、reference、eval 或测试信号。

## 失败归因

每次明显失败都应尽量归到一个主要原因：

- 请求不清：用户目标、范围或验收标准不足
- 路由不准：skill 的 `description` 或适用场景不清晰
- 上下文过载：rule 或主 skill 放入了过多低频资料
- 知识缺失：reference 缺少关键模块事实或历史坑点
- 工具失败：搜索、编辑、测试或外部工具不可用
- 验证不足：缺少能约束 AI 的测试、lint 或验收标准
- 模型不适配：同一 Harness 下某个模型稳定性明显更差

归因后再决定改哪里，不要把所有问题都通过增加 rule 解决。

## 治理节奏

- 新增 rule 前，先确认它是否稳定、强约束、always-on。
- 新增 skill 前，先确认现有 skill 是否可以扩展，避免路由重叠。
- 重构 skill 时，优先收紧 `description` 和主文件，再决定是否补 reference。
- 优先把“该读哪些 skill、最多读到哪一层”做成 rule 和结构约束，而不是让模型每次自由发挥。
- 定期用 `ai/evals/` 中的任务回放，验证 rule 和 skill 调整是否真的减少返工。
