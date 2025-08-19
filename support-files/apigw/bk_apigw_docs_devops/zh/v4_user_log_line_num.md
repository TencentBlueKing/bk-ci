### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/logs/last_line_num
### 资源描述
#### 获取当前构建的最大行号
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称        | 参数类型    | 必须  | 参数说明         |
| ----------- | ------- | --- | ------------ |
| archiveFlag | boolean |     | 是否查询归档数据     |
| buildId     | String  | √   | 构建ID (b-开头)  |
| pipelineId  | String  | √   | 流水线ID (p-开头) |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                            | 说明               |
| ------- | ----------------------------------------------- | ---------------- |
| default | [ResultQueryLogLineNum](#ResultQueryLogLineNum) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?archiveFlag={archiveFlag}&buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "buildId" : "",
    "finished" : false,
    "lastLineNum" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultQueryLogLineNum
##### 数据返回包装模型

| 参数名称    | 参数类型                                | 必须  | 参数说明 |
| ------- | ----------------------------------- | --- | ---- |
| data    | [QueryLogLineNum](#QueryLogLineNum) |     |      |
| message | string                              |     | 错误信息 |
| status  | integer                             | √   | 状态码  |

#### QueryLogLineNum
##### 日志行号查询模型

| 参数名称        | 参数类型    | 必须  | 参数说明   |
| ----------- | ------- | --- | ------ |
| buildId     | string  | √   | 构建ID   |
| finished    | boolean | √   | 是否结束   |
| lastLineNum | integer | √   | 日志存储状态 |

 
