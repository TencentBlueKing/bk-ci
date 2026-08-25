---
name: pipeline-template-module
description: 处理 BK-CI 流水线模板的创建、版本管理、实例化、权限控制、PAC 模板和商店集成时使用。当用户提到模板复用、模板版本、批量实例化、模板迁移、商店模板或 PAC 模板时优先使用。
---

# 流水线模板模块

## 适用场景

- 创建或维护流水线模板
- 模板实例化、批量实例化、模板更新
- 模板版本、草稿、发布、分支版本管理
- PAC 模板能力
- 模板权限控制和商店模板集成

## 不适用场景

- 只是修改单条流水线模型结构，不涉及模板抽象
- 只是 YAML 转换问题，不涉及模板生命周期
- 只是商店插件或普通流水线执行问题

## 快速指导

1. 先判断你遇到的是哪条主线：
   - 模板基础结构与版本体系：看 `reference/1-template-foundation.md`
   - 实例化、批量操作、权限：看 `reference/2-instance-version-permission.md`
   - PAC 模板与商店集成：看 `reference/3-pac-store-integration.md`
   - 排查、事件、锁与扩展：看 `reference/4-operations-extension.md`
2. 模板问题先分清 V1 还是 V2。新功能判断、复杂版本管理、草稿、PAC 场景通常优先看 V2。
3. 模板和普通流水线的最大区别不是“多了一份模型”，而是多了：
   - 版本语义
   - 实例关联关系
   - 权限与共享边界
   - 可能的商店分发与 PAC 来源
4. 只要改模板结构或模板版本逻辑，就要同时检查：
   - 模板资源持久化
   - 实例化结果
   - 版本兼容
   - 模板关联流水线的更新影响
5. PAC 模板、商店模板、项目内模板不要混成一个心智模型，它们的来源、权限和版本流转不同。

## 高信号规则

- 模板模块负责“如何复用与分发流水线定义”，`pipeline-model-architecture` 负责“模型本身长什么样”
- 模板版本和实例版本要区分，不要把模板版本号等同于流水线运行版本
- 批量实例化、批量更新、商店安装都会放大模板设计问题，改动前要先评估实例影响面
- PAC 模板会同时受到模板体系和 YAML 转换体系影响

## 关键陷阱

- 不区分 V1 / V2，就直接改模板逻辑，结果改错链路
- 只改模板本身，不看已经实例化出去的流水线如何受影响
- 把 PAC 模板问题误当成普通模板问题，或反过来
- 商店模板、项目模板、公共模板混用同一套权限和版本心智
- 模板变更只验证单模板，不验证批量实例、版本切换和历史兼容

## 延伸阅读

- 模板基础：`reference/1-template-foundation.md`
- 实例化与版本：`reference/2-instance-version-permission.md`
- PAC 与商店：`reference/3-pac-store-integration.md`
- 排查与扩展：`reference/4-operations-extension.md`
- 涉及模型结构时：再看 `pipeline-model-architecture`
- 涉及 YAML/PAC 转换细节时：再看 `yaml-pipeline-transfer`
