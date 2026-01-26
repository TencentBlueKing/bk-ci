---
name: managing-devops-pipeline
description: 管理蓝盾流水线的构建操作，包括查询构建历史、获取启动参数、查看构建状态、启动构建。当用户提及流水线、构建、部署、CI/CD、蓝盾或需要触发构建任务时使用。
---

# 蓝盾流水线管理

通过 MCP 工具 `devops-prod-pipeline` 管理蓝盾流水线构建。

## 核心概念

- **projectId**：项目英文名（如 `myproject`）
- **pipelineId**：流水线 ID，以 `p-` 开头（如 `p-abc123`）
- **buildId**：构建 ID，以 `b-` 开头（如 `b-xyz789`）

## 重要规则

**启动构建前必须获得用户确认**：在调用 `v4_user_build_start` 之前，必须向用户展示完整的构建入参并获得明确确认。未经用户确认，禁止执行构建操作。

## 常用工作流

### 1. 启动构建

```
步骤 1：获取启动参数 → devops-prod-pipeline:v4_user_build_startInfo
步骤 2：向用户展示构建参数，等待用户确认 ⚠️ 必须执行
步骤 3：用户确认后启动构建 → devops-prod-pipeline:v4_user_build_start
步骤 4：查看状态 → devops-prod-pipeline:v4_user_build_status
```

**步骤 2 确认模板**：
```
即将启动构建，请确认以下参数：
- 项目：{projectId}
- 流水线：{pipelineId}
- 构建参数：
  {列出所有 body_param 的 key-value}

是否确认启动？
```

### 2. 查询构建历史

使用 `devops-prod-pipeline:v4_user_build_list` 获取历史记录。

## 常用流水线

用户配置的常用流水线，参阅 [config.json](config.json)

**URL 解析规则**：从 `https://xxxxxx/console/pipeline/{projectId}/{pipelineId}` 提取：
- `projectId`：`/pipeline/` 后的第一段
- `pipelineId`：以 `p-` 开头的最后一段

## 工具参考

**获取构建历史**：参阅 [reference/build-list.md](reference/build-list.md)
**获取启动参数**：参阅 [reference/build-startinfo.md](reference/build-startinfo.md)
**查看构建状态**：参阅 [reference/build-status.md](reference/build-status.md)
**启动构建**：参阅 [reference/build-start.md](reference/build-start.md)

## 快速示例

### 查询最近构建

```json
{
  "path_param": { "projectId": "your-project" },
  "query_param": { "pipelineId": "p-xxx", "page": 1, "pageSize": 10 }
}
```

### 启动一次构建

```json
{
  "path_param": { "projectId": "your-project" },
  "query_param": { "pipelineId": "p-xxx" },
  "body_param": { "branch": "master" }
}
```

## 构建状态枚举

| 状态 | 说明 |
|------|------|
| SUCCEED | 成功 |
| FAILED | 失败 |
| CANCELED | 已取消 |
| RUNNING | 运行中 |
| QUEUE | 排队中 |
| STAGE_SUCCESS | 阶段成功 |
