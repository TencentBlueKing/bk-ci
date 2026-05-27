# 仓库基础与类型差异

## Repository 的核心职责

`Repository` 模块负责：

- 仓库资源接入
- 仓库类型差异封装
- 认证与授权信息管理
- Webhook 与提交记录
- PAC 相关仓库上下文

## 常见仓库类型

常见支持类型包括：

- 工蜂 Git
- TGit
- GitHub
- GitLab
- SVN
- P4

这些类型在下面几个方面通常不同：

- 认证方式
- 仓库标识字段
- Webhook 支持形态
- API 能力

## 数据模型理解

通常会有：

- 一个仓库主表
- 多个按类型拆分的明细表
- Token / OAuth 相关表
- 提交记录、Webhook 记录、SCM 配置表

排查时不要只看主表。很多问题都在类型明细表里。

## 什么时候切到别的 skill

- YAML / PAC 转换：`yaml-pipeline-transfer`
- 项目归属：`project-module-architecture`
- 权限模型：`auth-module-architecture`
