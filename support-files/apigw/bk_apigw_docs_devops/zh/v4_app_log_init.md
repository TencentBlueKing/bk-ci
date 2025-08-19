### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/logs/init_logs
### 资源描述
#### 根据构建ID获取初始化所有日志
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称            | 参数类型    | 必须  | 参数说明                     |
| --------------- | ------- | --- | ------------------------ |
| archiveFlag     | boolean |     | 是否查询归档数据                 |
| buildId         | String  | √   | 构建ID (b-开头)              |
| containerHashId | String  |     | 对应containerHashId (c-开头) |
| debug           | boolean |     | 是否包含调试日志                 |
| executeCount    | integer |     | 执行次数                     |
| jobId           | String  |     | 对应jobId                  |
| pipelineId      | String  |     | 流水线ID (p-开头)             |
| stepId          | String  |     | 对应stepId                 |
| tag             | String  |     | 对应elementId (e-开头)       |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                | 说明               |
| ------- | ----------------------------------- | ---------------- |
| default | [ResultQueryLogs](#ResultQueryLogs) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?archiveFlag={archiveFlag}&buildId={buildId}&containerHashId={containerHashId}&debug={debug}&executeCount={executeCount}&jobId={jobId}&pipelineId={pipelineId}&stepId={stepId}&tag={tag}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "buildId" : "",
    "finished" : false,
    "hasMore" : false,
    "logs" : [ {
      "containerHashId" : "",
      "executeCount" : 0,
      "jobId" : "",
      "lineNo" : 0,
      "message" : "",
      "priority" : "",
      "stepId" : "",
      "subTag" : "",
      "tag" : "",
      "timestamp" : 0
    } ],
    "message" : "",
    "status" : 0,
    "subTags" : [ "" ],
    "timeUsed" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultQueryLogs
##### 数据返回包装模型

| 参数名称    | 参数类型                    | 必须  | 参数说明 |
| ------- | ----------------------- | --- | ---- |
| data    | [QueryLogs](#QueryLogs) |     |      |
| message | string                  |     | 错误信息 |
| status  | integer                 | √   | 状态码  |

#### QueryLogs
##### 日志查询模型

| 参数名称     | 参数类型                      | 必须  | 参数说明     |
| -------- | ------------------------- | --- | -------- |
| buildId  | string                    | √   | 构建ID     |
| finished | boolean                   | √   | 是否结束     |
| hasMore  | boolean                   |     | 是否有后续日志  |
| logs     | List<[LogLine](#LogLine)> | √   | 日志列表     |
| message  | string                    |     | 错误信息     |
| status   | integer                   | √   | 日志查询状态   |
| subTags  | List<string>              |     | 日志子tag列表 |
| timeUsed | integer                   | √   | 所用时间     |

#### LogLine
##### 日志模型

| 参数名称            | 参数类型    | 必须  | 参数说明              |
| --------------- | ------- | --- | ----------------- |
| containerHashId | string  |     | 日志containerHashId |
| executeCount    | integer |     | 日志执行次数            |
| jobId           | string  | √   | 日志jobId           |
| lineNo          | integer | √   | 日志行号              |
| message         | string  | √   | 日志消息体             |
| priority        | string  | √   | 日志权重级             |
| stepId          | string  |     | 日志stepId          |
| subTag          | string  | √   | 日志子tag            |
| tag             | string  | √   | 日志tag             |
| timestamp       | integer | √   | 日志时间戳             |

 
