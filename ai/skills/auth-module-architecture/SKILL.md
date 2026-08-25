---
name: auth-module-architecture
description: 处理 BK-CI Auth 模块时使用，例如 RBAC 权限校验、用户组与资源管理、IAM 集成、授权迁移和 OAuth2 认证。当用户要改权限平台实现而不是单次权限模型变更时优先使用。
---

# Auth 模块架构

## 适用场景

- 修改 RBAC 权限校验和资源授权逻辑
- 处理用户组、成员、资源与权限关系
- 修改 IAM 集成、回调和同步逻辑
- 处理权限迁移、分级管理员或 OAuth2 认证

## 不适用场景

- 只是新增一种权限资源类型或动作模型
- 只是给用户授权、加成员、查权限
- 只是修改其他业务模块里的权限调用点
- 只是处理普通登录页面或前端鉴权展示

## 快速指导

1. 这个 skill 关注的是“权限平台本身如何实现和协作”，不是单次资源接入手册。
2. Auth 既包含 RBAC 资源授权，也包含 IAM 对接、用户组、OAuth2 等平台能力。
3. 先按问题类型进入对应参考文档：
   - 模块结构与核心对象：`reference/1-auth-foundation.md`
   - RBAC、用户组与授权链路：`reference/2-rbac-group-authorization.md`
   - IAM 集成、OAuth2 与排查：`reference/3-iam-oauth-debug.md`
4. 如果你在设计新的资源类型、动作列表或初始化流程，切到 `permission-model-change-guide`。
5. 如果你在处理业务模块里某个权限调用失败，先判断是 Auth 平台问题还是业务侧传参问题。

## 高信号规则

- Auth 的核心是资源、动作、用户组、成员和授权关系
- IAM 集成和本地 RBAC 数据是一条完整权限链，不应分开孤立看
- 用户组配置、权限校验和资源同步往往会一起影响结果
- 权限平台改动要优先考虑兼容性、缓存刷新和历史数据

## 关键陷阱

- 把权限模型变更和 Auth 平台实现混为一谈
- 只改校验逻辑，不看用户组、资源数据和同步链路
- OAuth2、回调、权限校验共用上下文时，忽略边界差异
- 权限问题排查只看接口返回，不看组、资源和动作三元关系

## 延伸阅读

- 模块结构与核心对象：`reference/1-auth-foundation.md`
- RBAC、用户组与授权链路：`reference/2-rbac-group-authorization.md`
- IAM 集成、OAuth2 与排查：`reference/3-iam-oauth-debug.md`
- 如果你在做权限模型接入：再看 `permission-model-change-guide`
