# Lightweight AI Workflow Pilot

## 背景

本文件用于试跑 `lightweight-ai-workflow`，验证轻量任务分流、impact checklist 和 verification checklist
是否能减少漏读 skill、漏影响面和验证不足。

## 试点任务

### 1. 单模块小改动

## 用户请求

为 SCC Task 和 SCC Scan Schema 补充 auth 模块 i18n 文案，来源参考
`openspec/changes/archive/2025-12-05-add-scc-i18n/proposal.md`。

## 期望 Harness 行为

- 应读取目标模块 skill，不应额外扩大到全局架构或多个 reference。
- 不应默认进入 OpenSpec 或多 Agent。
- 实现前应能说明本次影响面很小的依据。

## 验证信号

- 受影响模块的编译、单测、lint 或人工验收点至少有一个明确结果。
- 结论中说明未运行验证的原因和剩余风险。

## 评测记录

- 编辑器：
- 模型：
- 结果：待回放
- 返工原因：待回放后填写
- 应反哺到 rule / skill / reference / tests 的建议：待回放后填写

### 2. 跨模块中等需求

## 用户请求

统一 V2 模板版本名称重复处理逻辑，来源参考
`openspec/changes/archive/2026-03-13-unify-template-version-name-dedup/proposal.md`。

## 期望 Harness 行为

- 应读取目标模块 skill，必要时最多补一个横切 skill。
- 应先输出 impact checklist，再进入实现。
- 如果发现接口、数据结构、权限、迁移或回滚影响，应升级 OpenSpec。

## 验证信号

- impact checklist 覆盖受影响模块、调用方/被调用方和关键约束。
- verification checklist 说明已验证项、未验证项和剩余风险。

## 评测记录

- 编辑器：
- 模型：
- 结果：待回放
- 返工原因：待回放后填写
- 应反哺到 rule / skill / reference / tests 的建议：待回放后填写

### 3. 结构性 OpenSpec 变更

## 用户请求

新增 Creative Stream 资源类型并接入权限中心，来源参考
`openspec/changes/archive/2025-12-16-add-creative-stream-resource-type/proposal.md`。

## 期望 Harness 行为

- 应先确认是否存在 `openspec/changes/<change>/`。
- 如已有变更目录，应先更新 `proposal.md`、`design.md`、`tasks.md` 或 `specs/`。
- 不应在文档未同步时直接修改代码。

## 验证信号

- OpenSpec 文档和代码改动保持一致。
- 实现结论说明验证结果、未验证项和历史遗留问题。

## 评测记录

- 编辑器：
- 模型：
- 结果：待回放
- 返工原因：待回放后填写
- 应反哺到 rule / skill / reference / tests 的建议：待回放后填写
