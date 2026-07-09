# 获取流水线构建历史

工具：`devops-prod-pipeline:v4_user_build_list`

## 参数结构

```json
{
  "path_param": {
    "projectId": "项目英文名（必填）"
  },
  "query_param": {
    "pipelineId": "流水线ID，p-开头（必填）",
    "page": 1,
    "pageSize": 20,
    "status": "SUCCEED|FAILED|CANCELED|RUNNING|QUEUE|STAGE_SUCCESS",
    "trigger": "MANUAL|TIME_TRIGGER|WEB_HOOK|PIPELINE|REMOTE",
    "startUser": "启动人",
    "buildNoStart": 1,
    "buildNoEnd": 100,
    "startTimeStartTime": 1700000000000,
    "startTimeEndTime": 1700100000000,
    "endTimeStartTime": 1700000000000,
    "endTimeEndTime": 1700100000000,
    "archiveFlag": false
  }
}
```

## 必填参数

| 参数 | 位置 | 说明 |
|------|------|------|
| projectId | path_param | 项目英文名 |
| pipelineId | query_param | 流水线 ID（p-开头） |

## 可选参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | number | 1 | 页码 |
| pageSize | number | 20 | 每页条数（最大100） |
| status | string | - | 按状态筛选 |
| trigger | string | - | 按触发方式筛选 |
| startUser | string | - | 按启动人筛选 |
| buildNoStart | number | - | 构建号范围起始 |
| buildNoEnd | number | - | 构建号范围结束 |
| startTimeStartTime | number | - | 开始时间范围起始（13位时间戳） |
| startTimeEndTime | number | - | 开始时间范围结束（13位时间戳） |
| endTimeStartTime | number | - | 结束时间范围起始（13位时间戳） |
| endTimeEndTime | number | - | 结束时间范围结束（13位时间戳） |
| archiveFlag | boolean | false | 是否查询归档数据 |

## 示例

### 查询最近10次构建

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123", "page": 1, "pageSize": 10 }
}
```

### 查询失败的构建

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123", "status": "FAILED" }
}
```

### 查询某人触发的构建

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123", "startUser": "zhangsan" }
}
```
