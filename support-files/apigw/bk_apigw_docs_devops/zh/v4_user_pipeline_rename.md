### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/pipelines/pipeline_name
### 资源描述
#### 流水线重命名
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明  |
| ---------- | ------ | --- | ----- |
| pipelineId | String | √   | 流水线ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称  | 参数类型                          | 必须   |
| ----- | ----------------------------- | ---- |
| 流水线名称 | [PipelineName](#PipelineName) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "name" : "",
  "oldName" : ""
}
```

### default 返回样例

```Json
{
  "data" : false,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### PipelineName
##### 流水线模型-修改NAME

| 参数名称    | 参数类型   | 必须  | 参数说明      |
| ------- | ------ | --- | --------- |
| name    | string | √   | 流水线修改后的名称 |
| oldName | string | √   | 流水线修改前的名称 |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
