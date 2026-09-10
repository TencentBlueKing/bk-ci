# USER_AGENT API 说明（Dispatch Service）

> 来源：`https://dev-kubernetes.devops.woa.com/swagger/dev-rbac/dispatch/api/swagger.json`  
> Tag：`USER_AGENT`（用户-Agent）  
> 服务：DevOps Dispatch Service `4.0.0`  
> Base path：`/dispatch`

本文档供前端 TaskList 及相关 AI/开发任务对接使用。

## 通用约定

- **完整路径前缀**：`/dispatch` + 下文 path
- **通用 Header**：`X-DEVOPS-UID`（必填，用户 ID，示例：`admin`）

---

## 重要变更（必读）

> **已删除 / 废弃**：`GET /api/user/agents/listAgentPipelineJobs`（获取 agent 任务详情列表 - JOB 视图）  
> **替换为**：`POST /api/user/agents/listAgentPipeline`（获取 agent 任务详情列表）

迁移注意：

1. 方法从 **GET → POST**
2. 原 query 参数（`agentId` / `envId` / `page` / `pageSize` / `startTime` / `endTime` / `pipelineId` / `jobId` / `creator` / `taskStatus`）改为 **JSON Body**（`TPAPipelineReq`）
3. `projectId` 仍在 **query**
4. 新接口 Body 增加字段 **`view`**：`PIPELINE` | `JOB` | `BUILD`（用于切换视图，替代原 JOB 专用接口）
5. 前端 / 调用方不要再请求 `listAgentPipelineJobs`

---

## 1. GET `/api/user/agents/fetchAgentBuildsByBuild`

**说明**：根据 BuildId，获取 Agent 构建记录

### Query

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| projectId | 是 | string | 项目 ID |
| buildId | 是 | string | 筛选此 buildId |
| agentId | 否 | string | agent Hash ID |
| envId | 否 | string | env Hash ID |
| page | 否 | int32 | 第几页 |
| pageSize | 否 | int32 | 每页条数 |

### Response

类型：`ResultPageAgentPipelineContainerBuild`

```json
{
  "status": 0,
  "message": "string",
  "data": {
    "count": 0,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1,
    "records": [
      {
        "buildId": "string",
        "projectId": "string",
        "pipelineId": "string",
        "containerId": "string",
        "executeCount": 0,
        "status": "string",
        "startTime": "date-time",
        "endTime": "date-time",
        "buildNum": 0,
        "creator": "string",
        "tasks": [
          { "taskName": "string", "vmSeqId": "string", "stageId": "string" }
        ]
      }
    ]
  }
}
```

`records` 项类型：`AgentPipelineContainerBuild`  
必填字段：`buildId`, `buildNum`, `containerId`, `creator`, `endTime`, `executeCount`, `pipelineId`, `projectId`, `startTime`

---

## 2. GET `/api/user/agents/fetchAgentBuildsByJob`

**说明**：根据 JobId，获取 Agent 构建记录

### Query

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| projectId | 是 | string | 项目 ID |
| agentId | 否 | string | agent Hash ID |
| envId | 否 | string | env Hash ID |
| pipelineId | 否 | string | 筛选此 pipelineId |
| jobId | 否 | string | jobId |
| page | 否 | int32 | 第几页 |
| pageSize | 否 | int32 | 每页条数 |

### Response

同接口 1：`ResultPageAgentPipelineContainerBuild`

---

## 3. GET `/api/user/agents/fetchAgentBuildsByPipeline`

**说明**：根据 PipelineId，获取 Agent 构建记录

### Query

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| projectId | 是 | string | 项目 ID |
| pipelineId | 是 | string | 筛选此 pipelineId |
| agentId | 否 | string | agent Hash ID |
| envId | 否 | string | env Hash ID |
| page | 否 | int32 | 第几页 |
| pageSize | 否 | int32 | 每页条数 |

### Response

同接口 1：`ResultPageAgentPipelineContainerBuild`

---

## 4. POST `/api/user/agents/listAgentPipeline`

> **替代已删除的** `GET /api/user/agents/listAgentPipelineJobs`

**说明**：获取 agent 任务详情列表

### Query

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| projectId | 是 | string | 项目 ID |

### Body（`TPAPipelineReq`，`application/json`）

| 字段 | 类型 | 说明 |
|------|------|------|
| agentId | string | agent Hash ID |
| envId | string | env Hash ID |
| page | int32 | 第几页 |
| pageSize | int32 | 每页多少条 |
| startTime | int64 | 开始执行时间 |
| endTime | int64 | 结束执行时间 |
| pipelineId | string | pipeline ID |
| jobId | string | job ID |
| creator | string | 执行人 |
| taskStatus | enum | `QUEUE` \| `RUNNING` \| `DONE` \| `FAILURE` |
| view | enum | `PIPELINE` \| `JOB` \| `BUILD`（视图切换） |

### Response

类型：`ResultTPAPipelineBuildCountResp`

```json
{
  "status": 0,
  "message": "string",
  "data": {
    "pipelineCount": 0,
    "jobCount": 0,
    "buildCount": 0,
    "result": {
      "count": 0,
      "page": 1,
      "pageSize": 20,
      "totalPages": 1,
      "records": [
        {
          "pipelineId": "string",
          "pipelineName": "string",
          "jobId": "string",
          "jobName": "string",
          "buildCount": 0,
          "lastBuildTime": "date-time",
          "avgTimeInterval": 0,
          "lastContainerId": 0,
          "stageId": "string",
          "stageNumb": "string"
        }
      ]
    }
  }
}
```

`records` 项类型：`TPAPipelineBuild`

---

## 实现约束

1. 列表任务详情只调用 `POST /dispatch/api/user/agents/listAgentPipeline`，不要再调 `listAgentPipelineJobs`。
2. 需要 JOB 视图时传 `view: "JOB"`；PIPELINE / BUILD 同理。
3. 构建明细按维度分别用：
   - Build → `fetchAgentBuildsByBuild`
   - Job → `fetchAgentBuildsByJob`
   - Pipeline → `fetchAgentBuildsByPipeline`
4. 完整路径前缀为：`/dispatch` + 上述 path。
