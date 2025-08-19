### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/callbacks/callback_history
### 资源描述
#### callback回调执行历史记录
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | projectId  |

#### Query参数

| 参数名称      | 参数类型                                                                                                                                                                                                                                                                                                       | 必须  | 参数说明                        |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | --------------------------- |
| endTime   | String                                                                                                                                                                                                                                                                                                     |     | 结束时间(yyyy-MM-dd HH:mm:ss格式) |
| event     | ENUM(DELETE_PIPELINE, CREATE_PIPELINE, UPDATE_PIPELINE, STREAM_ENABLED, RESTORE_PIPELINE, BUILD_START, BUILD_END, BUILD_STAGE_START, BUILD_STAGE_END, BUILD_JOB_START, BUILD_JOB_END, BUILD_TASK_START, BUILD_TASK_END, BUILD_TASK_PAUSE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_ENABLE, PROJECT_DISABLE) | √   | 事件类型                        |
| page      | integer                                                                                                                                                                                                                                                                                                    |     | 第几页                         |
| pageSize  | integer                                                                                                                                                                                                                                                                                                    |     | 每页条数(默认20, 最大100)           |
| startTime | String                                                                                                                                                                                                                                                                                                     |     | 开始时间(yyyy-MM-dd HH:mm:ss格式) |
| url       | String                                                                                                                                                                                                                                                                                                     | √   | 回调url                       |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                                                  | 说明               |
| ------- | ------------------------------------------------------------------------------------- | ---------------- |
| default | [ResultPageProjectPipelineCallBackHistory](#ResultPageProjectPipelineCallBackHistory) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?endTime={endTime}&event={event}&page={page}&pageSize={pageSize}&startTime={startTime}&url={url}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ {
      "callBackUrl" : "",
      "createdTime" : 0,
      "endTime" : 0,
      "errorMsg" : "",
      "events" : "",
      "id" : 0,
      "projectId" : "",
      "requestBody" : "",
      "requestHeaders" : [ {
        "name" : "",
        "value" : ""
      } ],
      "responseBody" : "",
      "responseCode" : 0,
      "startTime" : 0,
      "status" : ""
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageProjectPipelineCallBackHistory
##### 数据返回包装模型

| 参数名称    | 参数类型                                                                      | 必须  | 参数说明 |
| ------- | ------------------------------------------------------------------------- | --- | ---- |
| data    | [PageProjectPipelineCallBackHistory](#PageProjectPipelineCallBackHistory) |     |      |
| message | string                                                                    |     | 错误信息 |
| status  | integer                                                                   | √   | 状态码  |

#### PageProjectPipelineCallBackHistory
##### 分页数据包装模型

| 参数名称       | 参数类型                                                                    | 必须  | 参数说明  |
| ---------- | ----------------------------------------------------------------------- | --- | ----- |
| count      | integer                                                                 | √   | 总记录行数 |
| page       | integer                                                                 | √   | 第几页   |
| pageSize   | integer                                                                 | √   | 每页多少条 |
| records    | List<[ProjectPipelineCallBackHistory](#ProjectPipelineCallBackHistory)> | √   | 数据    |
| totalPages | integer                                                                 | √   | 总共多少页 |

#### ProjectPipelineCallBackHistory
##### 项目的流水线回调历史

| 参数名称           | 参数类型                                    | 必须  | 参数说明     |
| -------------- | --------------------------------------- | --- | -------- |
| callBackUrl    | string                                  | √   | 回调url地址  |
| createdTime    | integer                                 |     | 创建时间     |
| endTime        | integer                                 | √   | 结束时间     |
| errorMsg       | string                                  |     | 错误信息     |
| events         | string                                  | √   | 事件       |
| id             | integer                                 |     | 流水线id    |
| projectId      | string                                  | √   | 项目id     |
| requestBody    | string                                  | √   | 请求body   |
| requestHeaders | List<[CallBackHeader](#CallBackHeader)> |     | 请求header |
| responseBody   | string                                  |     | 响应body   |
| responseCode   | integer                                 |     | 响应状态码    |
| startTime      | integer                                 | √   | 开始时间     |
| status         | string                                  | √   | 状态       |

#### CallBackHeader
##### 回调header 模型

| 参数名称  | 参数类型   | 必须  | 参数说明 |
| ----- | ------ | --- | ---- |
| name  | string | √   | 名字   |
| value | string | √   | 值    |

 
