---
name: pipeline-plugin-development
description: 开发 BK-CI 流水线插件时使用，例如新增 Atom、编写 `task.json`、定义输入输出、处理多语言运行时、调试和发布插件。当用户要做插件本体而不是执行器或流水线模型时优先使用。
---

# 流水线插件开发

## 适用场景

- 新增一个流水线插件
- 修改 `task.json` 配置和字段定义
- 设计插件输入、输出、错误码和敏感字段
- 选择 Java、Python、NodeJS、Golang 等插件实现方式
- 调试、测试、发布或排查插件行为

## 不适用场景

- 修改 Worker 如何执行插件
- 修改 Agent 如何拉起 Worker
- 修改 Dispatch 如何调度构建机
- 修改流水线模型、YAML 转换或模板体系

## 快速指导

1. 这个 skill 关注的是“插件本体怎么定义和交付”，不是“插件如何被执行”。
2. 先确认你是在开发 Atom，而不是在改执行器链路。
3. 开发插件时优先看三类信息：
   - 插件基础与目录：`reference/1-plugin-foundation.md`
   - `task.json`、输入输出与错误码：`reference/2-task-json-io.md`
   - 调试、发布与最佳实践：`reference/3-debug-publish-practice.md`
4. `task.json` 是插件契约，字段设计、运行时配置、敏感信息标记都应先在这里定清楚。
5. 输入输出要以“稳定契约”为目标，不要把临时实现细节暴露给使用者。
6. 运行时和目标平台要尽量前置明确，避免发布后再补兼容。
7. 如果问题发生在插件被下载、执行、打日志或归档产物的阶段，通常要切到 `worker-module-architecture`。

## 高信号规则

- 插件开发的核心对象是 `task.json` 和插件执行入口
- 输入字段、输出变量、错误码属于对使用者可见的契约
- 多语言只是实现手段，契约稳定性比语言选择更重要
- 插件发布后会被很多流水线复用，兼容性和向后兼容要优先考虑

## 关键陷阱

- 把执行器问题误判成插件问题
- `task.json` 塞入过多前端/运行时细节，导致难以维护
- 忽略敏感字段、默认值和校验规则
- 先写实现，后补契约，最后导致输入输出不稳定

## 延伸阅读

- 插件基础与目录：`reference/1-plugin-foundation.md`
- `task.json`、输入输出与错误码：`reference/2-task-json-io.md`
- 调试、发布与最佳实践：`reference/3-debug-publish-practice.md`
- 如果问题出在执行阶段：再看 `worker-module-architecture`
- 如果问题出在流水线模型：再看 `pipeline-model-architecture`
