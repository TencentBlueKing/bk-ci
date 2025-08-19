### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/manual_review
### 资源描述
#### 人工审核插件进行审核
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明        |
| ---------- | ------ | --- | ----------- |
| buildId    | String | √   | 构建ID（b-开头）  |
| elementId  | String |     | 步骤Id（e-开头）  |
| pipelineId | String |     | 流水线ID（p-开头） |
| stepId     | String |     | 对应stepId    |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称 | 参数类型                        | 必须   |
| ---- | --------------------------- | ---- |
| 审核信息 | [ReviewParam](#ReviewParam) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?buildId={buildId}&elementId={elementId}&pipelineId={pipelineId}&stepId={stepId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "buildId" : "",
  "desc" : "",
  "params" : [ {
    "chineseName" : "",
    "desc" : "",
    "key" : "",
    "options" : [ {
      "key" : "",
      "value" : ""
    } ],
    "required" : false,
    "value" : "",
    "valueType" : "enum",
    "variableOption" : ""
  } ],
  "pipelineId" : "",
  "projectId" : "",
  "reviewUsers" : [ "" ],
  "status" : "enum",
  "suggest" : ""
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
#### ReviewParam
##### 人工审核插件-审核信息

| 参数名称        | 参数类型                                          | 必须  | 参数说明  |
| ----------- | --------------------------------------------- | --- | ----- |
| buildId     | string                                        | √   | 构建Id  |
| desc        | string                                        |     | 描述    |
| params      | List<[ManualReviewParam](#ManualReviewParam)> | √   | 参数列表  |
| pipelineId  | string                                        | √   | 流水线Id |
| projectId   | string                                        | √   | 项目Id  |
| reviewUsers | List<string>                                  | √   | 审核人   |
| status      | ENUM(PROCESS, ABORT)                          |     | 审核结果  |
| suggest     | string                                        |     | 审核意见  |

#### ManualReviewParam
##### 人工审核-自定义参数

| 参数名称           | 参数类型                                                      | 必须  | 参数说明         |
| -------------- | --------------------------------------------------------- | --- | ------------ |
| chineseName    | string                                                    |     | 中文名称         |
| desc           | string                                                    |     | 参数描述         |
| key            | string                                                    | √   | 参数名          |
| options        | List<[ManualReviewParamPair](#ManualReviewParamPair)>     |     | 下拉框列表        |
| required       | boolean                                                   | √   | 是否必填         |
| value          | string                                                    |     | 参数内容(Any 类型) |
| valueType      | ENUM(string, textarea, boolean, enum, checkbox, multiple) | √   | 参数类型         |
| variableOption | string                                                    |     | 变量形式的options |

#### ManualReviewParamPair
##### 人工审核-自定义参数-下拉框列表剑

| 参数名称  | 参数类型   | 必须  | 参数说明 |
| ----- | ------ | --- | ---- |
| key   | string | √   | 参数名  |
| value | string | √   | 参数内容 |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
