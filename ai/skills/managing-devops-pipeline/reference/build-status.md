# 查看构建状态信息

工具：`devops-prod-pipeline:v4_user_build_status`

查看指定构建的详细状态信息，包括各阶段状态。

## 参数结构

```json
{
  "path_param": {
    "projectId": "项目英文名（必填）"
  },
  "query_param": {
    "buildId": "构建ID，b-开头（必填）"
  }
}
```

## 必填参数

| 参数 | 位置 | 说明 |
|------|------|------|
| projectId | path_param | 项目英文名 |
| buildId | query_param | 构建 ID（b-开头） |

## 返回信息

- 构建整体状态
- 各 Stage 状态（stageStatus）
- 执行时间
- 错误信息（如有）

## 示例

```json
{
  "path_param": { "projectId": "myproject" },
  "query_param": { "buildId": "b-xyz789" }
}
```

## 使用场景

1. **监控构建进度**：启动构建后轮询状态
2. **排查失败原因**：查看失败构建的详细信息
3. **确认构建完成**：等待构建结束后获取最终状态
