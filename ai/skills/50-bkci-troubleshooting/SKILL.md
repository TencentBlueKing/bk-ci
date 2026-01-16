---
name: 50-bkci-troubleshooting
description: >
  BK-CI 故障排查指南，涵盖构建失败、Agent 问题、网络问题、代码检查、质量红线、
  制品库、性能问题等常见故障的诊断方法和解决方案。当用户遇到 BK-CI 系统问题、
  构建异常、服务不可用等情况时使用。
---

# BK-CI 故障排查指南

## 概述

本指南提供 BK-CI 平台常见问题的诊断方法和解决方案，帮助快速定位和解决故障。

## 适用场景

当用户遇到以下问题时，应使用本 Skill：

- 构建失败、构建卡住、构建超时
- Agent 离线、无可用构建机
- 服务不可用、接口报错
- 性能问题、响应缓慢
- 权限问题、认证失败
- 代码检查失败、质量红线拦截
- 制品上传/下载失败
- 网络连接问题

## 快速诊断流程

```
问题发生
    │
    ├─ 1. 确定问题类型
    │   ├─ 构建相关 → 参考构建问题排查
    │   ├─ Agent 相关 → 参考 Agent 问题排查
    │   ├─ 服务相关 → 参考服务问题排查
    │   └─ 性能相关 → 参考性能问题排查
    │
    ├─ 2. 收集关键信息
    │   ├─ 错误码 / 错误信息
    │   ├─ 请求 ID (X-DEVOPS-RID)
    │   ├─ 发生时间
    │   └─ 相关 ID (项目/流水线/构建)
    │
    ├─ 3. 查看相关日志
    │   ├─ Gateway 日志
    │   ├─ 服务日志
    │   └─ Agent/Worker 日志
    │
    └─ 4. 根据排查文档处理
```

## 文档索引

### 核心排查文档

| 文档 | 说明 |
|------|------|
| [01-build-troubleshooting.md](reference/01-build-troubleshooting.md) | 构建问题排查 |
| [02-agent-troubleshooting.md](reference/02-agent-troubleshooting.md) | Agent 问题排查 |
| [03-codecc-troubleshooting.md](reference/03-codecc-troubleshooting.md) | 代码检查问题排查 |
| [04-quality-troubleshooting.md](reference/04-quality-troubleshooting.md) | 质量红线问题排查 |
| [05-network-troubleshooting.md](reference/05-network-troubleshooting.md) | 网络问题排查 |
| [06-artifactory-troubleshooting.md](reference/06-artifactory-troubleshooting.md) | 制品库问题排查 |
| [09-performance-troubleshooting.md](reference/09-performance-troubleshooting.md) | 性能问题排查 |

### 参考资料

| 文档 | 说明 |
|------|------|
| [10-error-codes.md](reference/10-error-codes.md) | 错误码参考手册 |
| [11-quick-checklist.md](reference/11-quick-checklist.md) | 快速排查检查清单 |

### 诊断脚本

| 脚本 | 说明 |
|------|------|
| [health-check.sh](scripts/health-check.sh) | 服务健康检查脚本 |
| [log-collector.sh](scripts/log-collector.sh) | 日志收集脚本 |
| [db-diagnostic.sh](scripts/db-diagnostic.sh) | 数据库诊断脚本 |

## 常见问题速查

### 构建失败

1. **检查构建日志** - 查看具体错误信息
2. **检查 Agent 状态** - 确认构建机可用
3. **检查插件配置** - 确认插件参数正确
4. **检查资源配额** - 确认未超过限制

详见 [构建问题排查](reference/01-build-troubleshooting.md)

### Agent 离线

1. **检查 Agent 进程** - `ps aux | grep devopsAgent`
2. **检查网络连接** - `curl http://gateway/ms/environment`
3. **检查配置文件** - `.agent.properties`
4. **查看 Agent 日志** - `/data/bkci/logs/agent/agent.log`

详见 [Agent 问题排查](reference/02-agent-troubleshooting.md)

### 服务不可用

1. **检查服务进程** - `ps aux | grep java`
2. **检查服务日志** - `/data/bkci/logs/ci/*/error.log`
3. **检查依赖组件** - MySQL, Redis, RabbitMQ
4. **检查网关配置** - Nginx 配置

详见 [网络问题排查](reference/05-network-troubleshooting.md)

### 性能问题

1. **检查系统资源** - CPU, 内存, 磁盘, 网络
2. **检查 JVM 状态** - GC, 堆内存, 线程
3. **检查数据库** - 慢查询, 连接数, 锁
4. **检查缓存** - Redis 命中率

详见 [性能问题排查](reference/09-performance-troubleshooting.md)

## 日志位置

| 组件 | 日志路径 |
|------|----------|
| Gateway | `/data/bkci/logs/gateway/` |
| 后端服务 | `/data/bkci/logs/ci/{service}/` |
| Agent | `/data/bkci/logs/agent/` |
| Worker | `/data/bkci/logs/worker/` |

## 常用命令

```bash
# 查看服务状态
systemctl status bkci-*

# 查看服务日志
tail -f /data/bkci/logs/ci/process/process-devops.log

# 查看 Agent 日志
tail -f /data/bkci/logs/agent/agent.log

# 健康检查
curl http://localhost:21912/api/health

# 数据库检查
mysql -e "SHOW PROCESSLIST"

# Redis 检查
redis-cli info
```

## 问题升级

如果按照排查文档无法解决问题，请收集以下信息提交工单：

1. 问题描述和复现步骤
2. 错误码和错误信息
3. 请求 ID (X-DEVOPS-RID)
4. 发生时间
5. 相关日志（使用 log-collector.sh 收集）
6. 系统环境信息

## 相关 Skill

- [00-bkci-global-architecture](../00-bkci-global-architecture/SKILL.md) - 全局架构
- [29-process-module-architecture](../29-process-module-architecture/SKILL.md) - Process 模块
- [35-dispatch-module-architecture](../35-dispatch-module-architecture/SKILL.md) - Dispatch 模块
- [43-agent-module-architecture](../43-agent-module-architecture/SKILL.md) - Agent 模块
