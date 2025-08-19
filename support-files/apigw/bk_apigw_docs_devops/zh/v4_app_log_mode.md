### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/logs/log_mode
### 资源描述
#### 获取插件的的日志状态
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称         | 参数类型    | 必须  | 参数说明               |
| ------------ | ------- | --- | ------------------ |
| archiveFlag  | boolean |     | 是否查询归档数据           |
| buildId      | String  | √   | 构建ID (b-开头)        |
| executeCount | integer |     | 执行次数               |
| pipelineId   | String  |     | 流水线ID (p-开头)       |
| stepId       | String  |     | 对应stepId           |
| tag          | String  | √   | 对应elementId (e-开头) |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                          | 说明               |
| ------- | --------------------------------------------- | ---------------- |
| default | [ResultQueryLogStatus](#ResultQueryLogStatus) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?archiveFlag={archiveFlag}&buildId={buildId}&executeCount={executeCount}&pipelineId={pipelineId}&stepId={stepId}&tag={tag}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "buildId" : "",
    "finished" : false,
    "logMode" : "enum"
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultQueryLogStatus
##### 数据返回包装模型

| 参数名称    | 参数类型                              | 必须  | 参数说明 |
| ------- | --------------------------------- | --- | ---- |
| data    | [QueryLogStatus](#QueryLogStatus) |     |      |
| message | string                            |     | 错误信息 |
| status  | integer                           | √   | 状态码  |

#### QueryLogStatus
##### 日志状态查询模型

| 参数名称     | 参数类型                          | 必须  | 参数说明   |
| -------- | ----------------------------- | --- | ------ |
| buildId  | string                        | √   | 构建ID   |
| finished | boolean                       | √   | 是否结束   |
| logMode  | ENUM(UPLOAD, LOCAL, ARCHIVED) | √   | 日志存储状态 |

 
