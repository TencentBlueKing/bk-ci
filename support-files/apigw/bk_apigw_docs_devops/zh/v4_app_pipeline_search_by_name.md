### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/pipelines/search_by_name
### 资源描述
#### 根据流水线名称搜索
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称         | 参数类型   | 必须  | 参数说明 |
| ------------ | ------ | --- | ---- |
| pipelineName | String |     | 搜索名称 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                        | 说明               |
| ------- | ----------------------------------------------------------- | ---------------- |
| default | [ResultListPipelineIdAndName](#ResultListPipelineIdAndName) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineName={pipelineName}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "channelCode" : "enum",
    "pipelineId" : "",
    "pipelineName" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListPipelineIdAndName
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | List<[PipelineIdAndName](#PipelineIdAndName)> |     | 数据   |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### PipelineIdAndName
##### 流水线名称与Id

| 参数名称         | 参数类型                                                       | 必须  | 参数说明  |
| ------------ | ---------------------------------------------------------- | --- | ----- |
| channelCode  | ENUM(BS, AM, CODECC, GCLOUD, GIT, GONGFENGSCAN, CODECC_EE) |     | 渠道代码  |
| pipelineId   | string                                                     | √   | 流水线Id |
| pipelineName | string                                                     | √   | 流水线名称 |

 
