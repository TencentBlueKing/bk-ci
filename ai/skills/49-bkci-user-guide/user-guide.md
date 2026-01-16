# BK-CI 用户使用指南

## 概述

本指南为 BK-CI（蓝盾）用户提供全面的使用说明，涵盖从基础概念到高级功能的完整使用流程。

## 目录

### 第一部分：基础入门

1. [**BK-CI 基础概念与架构**](./reference/01-basic-concepts-guide.md)
   - BK-CI 核心概念
   - 系统架构概览
   - 基本术语解释

2. [**项目与权限管理**](./reference/02-project-permission-guide.md)
   - 项目创建与配置
   - 用户权限管理
   - 角色与权限分配

3. [**代码库接入与管理**](./reference/03-repository-management-guide.md)
   - 代码库接入配置
   - Webhook 触发器设置
   - 多代码库管理

### 第二部分：流水线操作

4. [**流水线创建与配置**](./reference/04-pipeline-creation-guide.md)
   - 流水线基础创建
   - 阶段和任务配置
   - 插件使用指南

5. [**构建环境与执行**](./reference/05-build-environment-guide.md)
   - 构建机环境配置
   - 构建任务执行
   - 环境变量管理

### 第三部分：高级功能

6. [**制品管理与发布**](./reference/06-artifact-management-guide.md)
   - 制品库使用
   - 制品上传下载
   - 版本管理策略

7. [**质量红线与代码检查**](./reference/07-quality-gate-guide.md)
   - 质量门禁配置
   - 代码扫描集成
   - 质量规则管理

8. [**通知与审批管理**](./reference/08-notification-approval-guide.md)
   - 通知服务配置
   - 审批流程设置
   - 消息模板管理

### 第四部分：运维监控

9. [**监控与日志管理**](./reference/09-monitoring-logging-guide.md)
   - 构建日志查看
   - 性能监控配置
   - 日志分析方法

10. [**故障排查与问题解决**](./reference/10-troubleshooting-guide.md)
    - 常见问题解决
    - 故障排查流程
    - 最佳实践建议

## 快速导航

### 新用户入门
1. 阅读 [基础概念与架构](./reference/01-basic-concepts-guide.md) 了解 BK-CI
2. 参考 [项目与权限管理](./reference/02-project-permission-guide.md) 创建项目
3. 按照 [流水线创建与配置](./reference/04-pipeline-creation-guide.md) 创建第一条流水线

### 常用操作
- **创建流水线**: [流水线创建与配置](./reference/04-pipeline-creation-guide.md)
- **配置构建环境**: [构建环境与执行](./reference/05-build-environment-guide.md)
- **管理制品**: [制品管理与发布](./reference/06-artifact-management-guide.md)
- **设置质量门禁**: [质量红线与代码检查](./reference/07-quality-gate-guide.md)

### 问题解决
- **构建失败**: [故障排查与问题解决](./reference/10-troubleshooting-guide.md)
- **权限问题**: [项目与权限管理](./reference/02-project-permission-guide.md)
- **日志查看**: [监控与日志管理](./reference/09-monitoring-logging-guide.md)

## 使用建议

### 学习路径
1. **初学者**: 按顺序阅读第一部分（基础入门）
2. **有经验用户**: 直接查阅相关章节
3. **管理员**: 重点关注权限管理和监控章节

### 最佳实践
- 在创建流水线前，先了解项目结构和权限配置
- 合理使用环境变量和参数化构建
- 定期检查和优化流水线性能
- 建立完善的通知和审批机制

## 相关资源

- [BK-CI 官方文档](https://docs.bkci.net/)
- [插件开发指南](../47-pipeline-plugin-development/)
- [故障排查专题](../50-bkci-troubleshooting/)

---

*本指南基于 BK-CI 最新版本编写，如有疑问请参考官方文档或联系技术支持。*