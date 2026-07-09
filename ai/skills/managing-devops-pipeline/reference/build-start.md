# 启动构建

工具：`devops-prod-pipeline:v4_user_build_start`

手动触发流水线构建。

## ⚠️ 重要：必须先获得用户确认

**在调用此工具前，必须向用户展示完整的构建参数并获得明确确认。未经确认禁止执行。**

确认内容必须包括：
- 项目 ID（projectId）
- 流水线 ID（pipelineId）
- 所有构建入参（body_param 的完整内容）

## 参数结构

```json
{
  "path_param": {
    "projectId": "项目英文名（必填）"
  },
  "query_param": {
    "pipelineId": "流水线ID，p-开头（必填）",
    "buildNo": 123
  },
  "body_param": {
    "参数key1": "参数value1",
    "参数key2": "参数value2"
  }
}
```

## 必填参数

| 参数 | 位置 | 说明 |
|------|------|------|
| projectId | path_param | 项目英文名 |
| pipelineId | query_param | 流水线 ID（p-开头） |

## 可选参数

| 参数 | 位置 | 类型 | 说明 |
|------|------|------|------|
| buildNo | query_param | number | 手动指定构建版本号 |
| body_param | body | object | 流水线入参，无参数时传空对象 `{}` |

## 工作流

```
1. 调用 v4_user_build_startInfo 获取需要的参数
2. 准备 body_param 填入参数值
3. ⚠️ 向用户展示完整参数，等待用户确认
4. 用户确认后调用 v4_user_build_start 启动构建
5. 使用返回的 buildId 调用 v4_user_build_status 监控状态
```

## 示例

### 无参数启动

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123" },
  "body_param": {}
}
```

### 带参数启动

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123" },
  "body_param": {
    "branch": "master",
    "env": "production",
    "version": "1.0.0"
  }
}
```

### 指定构建号启动

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "pipelineId": "p-abc123", "buildNo": 100 },
  "body_param": {}
}
```

## 注意事项

- **⚠️ 必须在启动前获得用户对构建参数的明确确认**
- `body_param` 的 key 需要与流水线定义的参数名一致
- 先调用 `v4_user_build_startInfo` 确认参数要求
- 返回的 `buildId` 用于后续状态查询
