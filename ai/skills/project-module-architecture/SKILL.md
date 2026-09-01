---
name: project-module-architecture
description: 处理 BK-CI 项目创建、项目属性、成员权限、项目标签、项目分片和项目资源边界时使用。当用户提到项目管理、projectId 含义、项目配置、成员管理或项目迁移时优先使用。
---

# Project 模块架构

## 适用场景

- 创建、更新或迁移项目
- 处理项目属性、标签、渠道、审批状态
- 排查成员、权限、项目资源边界
- 判断其他模块里 `projectId` 的真实含义
- 处理项目分片、路由、项目级基础配置

## 不适用场景

- 只是某个流水线、代码库或制品本身的业务逻辑
- 只是普通权限模型设计，不涉及项目这个资源域
- 只是数据库脚本设计，不涉及项目模块语义

## 快速指导

1. 先确认问题属于哪条主线：
   - 项目基础心智：看 `reference/1-project-foundation.md`
   - 项目属性、权限、分片与扩展：看 `reference/2-project-operations.md`
2. BK-CI 里绝大多数业务接口说的 `projectId`，实际都更接近 `T_PROJECT.english_name`，不是数据库里的 UUID 主键字段。
3. `Project` 是基础模块，很多问题看起来发生在别的模块，但根源是项目属性、项目启停状态、项目边界或项目权限没对齐。
4. 只要改项目字段、项目属性或项目路由逻辑，就要同时考虑对其他模块的影响。

## 高信号规则

- 项目是 BK-CI 资源隔离的基础边界
- 其他模块依赖 `Project`，但不应该绕过它自行定义项目语义
- 项目属性常常决定后续流水线、代码库、权限和 PAC 行为
- 项目分片和路由属于基础设施能力，不是普通业务字段

## 关键陷阱

- 把 `T_PROJECT.PROJECT_ID` 和业务里的 `projectId` 混用
- 只改项目表，不同步看项目属性和下游依赖模块
- 把项目问题误判为权限问题，或把权限问题误判为项目不存在
- 忽略项目分片、渠道和审批状态对整体行为的影响

## 延伸阅读

- 项目基础：`reference/1-project-foundation.md`
- 项目属性与分片：`reference/2-project-operations.md`
- 涉及全局模块协作时：再看 `00-bkci-global-architecture`
- 涉及权限实现时：再看 `auth-module-architecture`
