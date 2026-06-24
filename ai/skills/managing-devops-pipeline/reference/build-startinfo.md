# 获取流水线手动启动参数

工具：`devops-prod-pipeline:v4_user_build_startInfo`

获取流水线启动时需要填写的参数信息。在启动构建前调用此接口了解需要传递哪些参数。

## 参数结构

```json
{
  "path_param": {
    "projectId": "项目英文名（必填）"
  },
  "query_param": {
    "pipelineId": "流水线ID（必填）"
  }
}
```

## 必填参数

| 参数 | 位置 | 说明 |
|------|------|------|
| projectId | path_param | 项目英文名 |
| pipelineId | query_param | 流水线 ID（p-开头） |

## 使用场景

1. **启动构建前**：先调用此接口获取参数列表
2. **了解参数要求**：查看哪些参数是必填的
3. **获取默认值**：了解参数的默认值

## 示例

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123" }
}
```

## 工作流

```
1. 调用 v4_user_build_startInfo 获取参数定义
2. 根据返回的参数列表准备 body_param
3. 调用 v4_user_build_start 启动构建
```
