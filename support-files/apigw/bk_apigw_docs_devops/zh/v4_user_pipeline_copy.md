### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/pipelines/pipeline_copying
### 资源描述
#### 复制流水线编排
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明  |
| ---------- | ------ | --- | ----- |
| pipelineId | String | √   | 流水线模型 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称    | 参数类型                          | 必须   |
| ------- | ----------------------------- | ---- |
| 流水线COPY | [PipelineCopy](#PipelineCopy) | true |

#### 响应参数

| HTTP代码  | 参数类型                                  | 说明               |
| ------- | ------------------------------------- | ---------------- |
| default | [ResultPipelineId](#ResultPipelineId) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "desc" : "",
  "labels" : [ "" ],
  "name" : "",
  "staticViews" : [ "" ]
}
```

### default 返回样例

```Json
{
  "data" : {
    "id" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### PipelineCopy
##### 流水线-COPY创建信息

| 参数名称        | 参数类型         | 必须  | 参数说明              |
| ----------- | ------------ | --- | ----------------- |
| desc        | string       |     | 描述                |
| labels      | List<string> | √   | 流水线基础设置-基本信息中的标签  |
| name        | string       | √   | 名称                |
| staticViews | List<string> | √   | 配置静态流水线组，需要填写视图ID |

#### ResultPipelineId
##### 数据返回包装模型

| 参数名称    | 参数类型                      | 必须  | 参数说明 |
| ------- | ------------------------- | --- | ---- |
| data    | [PipelineId](#PipelineId) |     |      |
| message | string                    |     | 错误信息 |
| status  | integer                   | √   | 状态码  |

#### PipelineId
##### 流水线模型-ID

| 参数名称 | 参数类型   | 必须  | 参数说明  |
| ---- | ------ | --- | ----- |
| id   | string | √   | 流水线ID |

 
