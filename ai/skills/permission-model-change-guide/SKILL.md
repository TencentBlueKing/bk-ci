---
name: permission-model-change-guide
description: 修改 BK-CI IAM RBAC 权限模型时使用，例如新增资源类型、设计操作列表、配置 IAM 资源、补迁移数据和验证回调链路。当用户要变更权限模型而不是普通权限调用时优先使用。
---

# 权限模型变更指南

## 适用场景

- 新增一种 IAM 资源类型
- 为资源类型设计或调整操作列表
- 修改 IAM RBAC 配置、实例选择器或回调数据
- 补权限迁移、初始化脚本或线上变更流程
- 排查资源类型接入不完整的问题

## 不适用场景

- 只是调用现有权限校验接口
- 只是处理用户授权、加成员、移成员
- 只是修改 Auth 模块普通业务逻辑
- 只是做 OAuth2 或登录认证

## 快速指导

1. 这个 skill 关注的是“权限模型本身如何变更”，不是“现有权限怎么调用”。
2. 一次变更通常至少覆盖四层：资源定义、操作定义、数据初始化、校验验证。
3. 先按问题类型进入对应参考文档：
   - 资源类型与操作设计：`reference/1-resource-action-design.md`
   - IAM 配置、迁移与初始化：`reference/2-iam-config-migration.md`
   - 回调、验证与常见坑：`reference/3-callback-validation.md`
4. `create` 一类动作往往是权限模型里最容易配错的特殊项，要先确认关联资源是否还是父级资源。
5. 任何模型变更都要同步考虑国际化、初始化脚本、历史数据兼容和线上落地方式。
6. 如果你改的是 Auth 模块内部 RBAC 服务实现，而不是资源模型本身，切到 `auth-module-architecture`。

## 高信号规则

- 权限模型变更本质上是契约变更，不只是加几个枚举或 JSON
- 资源类型、操作列表、实例选择器、回调链路必须一起看
- 线上接入时要优先考虑初始化与兼容，而不是只让本地跑通
- API 方式和脚本方式都要明确边界，避免重复写入或状态不一致

## 关键陷阱

- 只加资源枚举，不补 IAM 配置和初始化链路
- `create`、`list`、`manage` 等动作的关联资源设计错误
- 只改配置，不做国际化、回调和验证
- 把权限模型问题误判成普通 Auth 业务问题

## 延伸阅读

- 资源类型与操作设计：`reference/1-resource-action-design.md`
- IAM 配置、迁移与初始化：`reference/2-iam-config-migration.md`
- 回调、验证与常见坑：`reference/3-callback-validation.md`
- 如果你在改 Auth 内部实现：再看 `auth-module-architecture`
