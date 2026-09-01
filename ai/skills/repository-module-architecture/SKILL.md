---
name: repository-module-architecture
description: 处理 BK-CI 代码库接入、代码库授权、Webhook、SCM 集成、PAC 开关与仓库类型差异时使用。当用户提到 Git/SVN/GitHub/TGit、Webhook、仓库认证、PAC 代码库或提交记录时优先使用。
---

# Repository 模块架构

## 适用场景

- 接入或管理代码库
- 处理仓库认证、OAuth、SSH、Token
- 配置或排查 Webhook
- 处理 PAC 代码库与提交记录
- 判断某种代码库类型应该落在哪套服务实现

## 不适用场景

- 只是普通 Git 命令使用问题
- 只是 PAC 的 YAML 转换问题，不涉及代码库接入
- 只是权限模型定义，不涉及仓库资源本身

## 快速指导

1. 先判断问题属于哪条主线：
   - 仓库基础与类型差异：看 `reference/1-repository-foundation.md`
   - Webhook、PAC、认证与排查：看 `reference/2-webhook-pac-auth.md`
2. Repository 模块的核心不是“执行 Git 操作”，而是“管理仓库资源、认证方式、Webhook 和平台接入语义”。
3. 只要改仓库类型、认证逻辑或 PAC 开关，就要同时看仓库明细表、认证方式和 Webhook 行为。
4. PAC 问题如果已经进入 YAML 解析或模板展开层，接下来切到 `yaml-pipeline-transfer`。

## 高信号规则

- `Repository` 负责仓库资源接入与管理，不负责流水线模型本身
- 不同仓库类型在认证、字段和 API 能力上都不完全一致
- `PROJECT_ID` 仍然要按项目英文标识理解
- PAC 是仓库侧开关和上下文来源，不等于 YAML 转换实现本身

## 关键陷阱

- 把代码库类型差异当成一套统一逻辑硬处理
- 只看主表，不看各仓库类型的明细表
- 把 Webhook 问题误判为仓库保存问题，或反过来
- PAC 开关开了就默认一切 YAML / 触发都能工作

## 延伸阅读

- 仓库基础：`reference/1-repository-foundation.md`
- Webhook 与 PAC：`reference/2-webhook-pac-auth.md`
- 涉及 YAML/PAC 转换时：再看 `yaml-pipeline-transfer`
- 涉及项目语义时：再看 `project-module-architecture`
