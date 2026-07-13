---
name: pipeline-model-interpreter
description: 解释 BK-CI 流水线或创作流的 Model JSON，帮助 AI 理解 Model -> Stage -> Container(Job) -> Element 的含义、启动参数、矩阵、Finally、审核与常见插件语义。用户贴出流水线 JSON、创作流 JSON、编排数据，或询问某个 Stage/Job/插件/atomCode/stepId 含义时使用。
---

# 流水线模型解释

## 适用场景

- 用户直接贴出 BK-CI 流水线或创作流的 `Model` JSON
- 用户问“这个流水线做了什么”“这个编排什么意思”“帮我理解这个 Stage/Job/插件”
- 用户要解释启动参数、矩阵、Finally、审核、脚本插件、商店插件输入输出
- 用户给出 `atomCode`、`elementId`、`stepId`、`jobId` 等信息，要求说明其在编排中的含义

## 不适用场景

- 修改模型代码、加字段、做兼容性改造
- 排查构建运行时失败日志或执行状态
- 设计固定摘要模板输出
- 需要理解 YAML 转换、持久化、版本兼容实现细节

## 快速指导

1. 先把 JSON 还原成 `Model -> Stage -> Container -> Element` 四层结构。
2. 第一个 `Stage` 里的 `TriggerContainer.params` 优先解释为启动参数入口；触发器插件本身通常不用当业务步骤展开。
3. 先判断容器类型：`trigger`、`vmBuild`、`normal`，再判断插件类型：`linuxScript`、`windowsScript`、`marketBuild`、`marketBuildLess`、`subPipelineCall`、`manualReviewUserTask` 等。
4. 解释插件时同时看 `@type`、`atomCode`、`name`、`data.input`、`data.output` 和 `additionalOptions`。
5. 如果用户问整体含义，先说明主流程、关键 Stage/Job、启动参数入口和可能产出；如果只问某个节点，只解释相关切片。
6. 遇到未知 `atomCode` 时，不臆测功能；明确标注“需结合插件文档确认”，并列出关键输入输出字段。
7. 解释某节点的执行时机时，结合 `runCondition` 判断；Element / Job / Stage 三层各有独立枚举，不要混用。
8. 详细分析步骤、常见插件速查和参数类型速查看 `reference/`。

## 高信号规则

- 创作流与流水线使用同一套模型结构，这个 skill 同时适用于两者
- `containerHashId`、`jobId`、`element.id`、`stepId` 语义不同，不能混用
- `status`、`startEpoch`、`elapsed`、`executeCount`、`timeCost` 等运行态字段默认忽略，除非用户明确问运行状态
- `additionalOptions.enable=false`、`jobControlOption.enable=false`、`stageControlOption.enable=false` 要明确标注为已禁用
- 敏感参数或 `PASSWORD` 类型只标注为敏感，不回显默认值

## 关键陷阱

- 把 TriggerContainer 里的触发器插件当成业务主流程
- 把运行态字段当成编排语义来解释
- 忽略矩阵展开后的子 Job、Finally Stage、审核控制和互斥组
- 只看 `atomCode` 不看 `data.input` / `data.output`，导致插件理解失真
- 对未知商店插件直接猜功能，而不是保守说明边界

## 延伸阅读

- 详细分析步骤：`reference/1-json-analysis-playbook.md`
- 容器、插件与参数类型速查：`reference/2-common-plugin-and-container-hints.md`
- classType / dispatchType / 枚举字面量清单：`reference/3-classtype-and-enum-catalog.md`
- 如果用户手里没有 Model JSON、想先取回来，可用 `managing-devops-pipeline` 或构建 Agent 的编排查询工具（获取流水线编排摘要/获取流水线编排）
- 如果需要理解模型代码结构、版本兼容或 YAML 转换：`pipeline-model-architecture`
