---
name: lightweight-ai-workflow
description: 处理 BK-CI AI 编码任务分流时使用，例如判断日常任务是否需要 OpenSpec、多 Agent、影响面清单或验证清单。
---

# Lightweight AI Workflow

## 适用场景

- 用户要求制定、执行或检查 AI 编码流程
- 需求开始前需要判断走轻流程、OpenSpec 还是多 Agent
- 代码实现前需要做影响面分析
- 代码实现后需要给出最小验证结论和剩余风险

## 不适用场景

- 已经明确是某个业务模块的普通实现，优先读该模块 skill
- 已有 `openspec/changes/<change>/`，优先遵守 `openspec-first`
- 用户只问概念、代码解释或不涉及交付流程的问题

## 快速指导

1. 先按 `skill-context-first` 识别主模块，默认只读 `1` 个模块主 skill。
2. 单模块、低风险、边界明确的任务，可按复杂度选择直接实现，或先进入 Plan Mode 梳理方案；不要
   直接升级 OpenSpec 或多 Agent。
3. 多模块、影响不明或容易漏联动点的任务，先完成 impact checklist，再决定是否实现。
4. 命中 OpenSpec 触发条件时，先补文档，再改代码。
5. 需求大、范围模糊、跨端或连续返工时，才升级多 Agent。
6. 实现完成后，输出 verification checklist，说明已验证项、未验证项和剩余风险。

## 任务分流

- **轻流程**：单模块、小范围、低风险、验收清楚；读取目标模块 skill 后，可按复杂度选择直接实现，或
  先进入 Plan Mode 梳理范围、影响面和方案。
- **影响面先行**：跨模块、调用链不明、配置/权限/数据/i18n 可能受影响；先写 impact checklist。
- **OpenSpec**：新增或修改 API、数据结构、数据库脚本、稳定外部契约；跨 2 个以上微服务或核心模块；
  涉及权限、兼容性、迁移、灰度、回滚；或验收口径不清。
- **多 Agent**：需求范围大且模块归属不清；需要需求、方案、实现、质量门禁分阶段交接；单 Agent
  连续两轮仍无法收敛；或用户明确要求多 Agent 协作。

## Impact Checklist

- 本次主要落在哪个模块 skill？是否需要补读 `1` 个横切 skill？
- 影响哪些层：Resource、Service、DAO、Model、配置、脚本、前端、Worker、Agent？
- 是否影响 API、数据契约、数据库、权限、i18n、事件、缓存或定时任务？
- 是否存在调用方、被调用方、历史兼容、灰度或回滚要求？
- 是否需要 OpenSpec 或多 Agent 升级？

## Verification Checklist

- 本次需求点是否逐项覆盖？
- 受影响模块是否至少跑过编译、单测、lint 或关键脚本之一？
- 没跑的验证是什么，原因是什么？
- 是否有新增 warning、失败或规则违反？
- 是否仍有未验证风险、历史遗留问题或需要人工确认的点？

## 高信号规则

- 轻量入口只负责分流和清单，不承载模块知识。
- 不要为了“保险”扩大流程；小任务优先把 token 用在代码和验证上。
- 发现重复漏改或流程误判时，先沉淀到 `ai/evals/`，确认稳定后再调整 rule 或 skill。

## 延伸阅读

- `skill-context-first`：模块 skill 读取预算
- `openspec-first`：已有 OpenSpec 变更的文档优先规则
- `multi-agent-workflow`：大任务或多 Agent 协作规则
- `../../HARNESS.md`：rule、skill、reference 与编辑器适配目录边界
