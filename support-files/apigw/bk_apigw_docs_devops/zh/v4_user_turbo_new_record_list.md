### 请求方法/请求路径
#### POST /{apigwType}/v4/turbo/projectId/{projectId}/history_list
### 资源描述
#### 新版编译加速获取加速历史列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明          |
| --------- | ------ | --- | ------------- |
| projectId | String | √   | 蓝盾项目ID(项目英文名) |

#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明  |
| --------- | ------- | --- | ----- |
| pageNum   | integer |     | 页数    |
| pageSize  | integer |     | 每页多少条 |
| sortField | String  |     | 排序字段  |
| sortType  | String  |     | 排序类型  |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称         | 参数类型                                  | 必须   |
| ------------ | ------------------------------------- | ---- |
| 编译加速历史请求数据信息 | [TurboRecordModel](#TurboRecordModel) | true |

#### 响应参数

| HTTP代码  | 参数类型                                                                  | 说明               |
| ------- | --------------------------------------------------------------------- | ---------------- |
| default | [ResponsePageTurboRecordHistoryVO](#ResponsePageTurboRecordHistoryVO) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?pageNum={pageNum}&pageSize={pageSize}&sortField={sortField}&sortType={sortType}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "clientIp" : [ "" ],
  "endTime" : "string",
  "pipelineId" : [ "" ],
  "projectId" : "",
  "startTime" : "string",
  "status" : [ "" ],
  "turboPlanId" : [ "" ]
}
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ {
      "buildId" : "",
      "clientIp" : "",
      "devopsBuildId" : "",
      "estimateTimeSecond" : 0,
      "estimateTimeValue" : "",
      "executeNum" : 0,
      "executeTimeSecond" : 0,
      "executeTimeValue" : "",
      "id" : "",
      "message" : "",
      "pipelineId" : "",
      "pipelineName" : "",
      "projectId" : "",
      "startTime" : "string",
      "status" : "",
      "turboPlanId" : "",
      "turboRatio" : ""
    } ],
    "totalPages" : 0
  },
  "message" : ""
}
```

### 相关模型数据
#### TurboRecordModel
##### 

| 参数名称        | 参数类型         | 必须  | 参数说明 |
| ----------- | ------------ | --- | ---- |
| clientIp    | List<string> |     |      |
| endTime     | string       |     |      |
| pipelineId  | List<string> |     |      |
| projectId   | string       |     |      |
| startTime   | string       |     |      |
| status      | List<string> |     |      |
| turboPlanId | List<string> |     |      |

#### ResponsePageTurboRecordHistoryVO
##### 

| 参数名称    | 参数类型                                                  | 必须  | 参数说明 |
| ------- | ----------------------------------------------------- | --- | ---- |
| code    | integer                                               |     |      |
| data    | [PageTurboRecordHistoryVO](#PageTurboRecordHistoryVO) |     |      |
| message | string                                                |     |      |

#### PageTurboRecordHistoryVO
##### 分页数据包装模型

| 参数名称       | 参数类型                                                | 必须  | 参数说明  |
| ---------- | --------------------------------------------------- | --- | ----- |
| count      | integer                                             | √   | 总记录行数 |
| page       | integer                                             | √   | 第几页   |
| pageSize   | integer                                             | √   | 每页多少条 |
| records    | List<[TurboRecordHistoryVO](#TurboRecordHistoryVO)> | √   | 数据    |
| totalPages | integer                                             | √   | 总共多少页 |

#### TurboRecordHistoryVO
##### 数据

| 参数名称               | 参数类型    | 必须  | 参数说明 |
| ------------------ | ------- | --- | ---- |
| buildId            | string  |     |      |
| clientIp           | string  |     |      |
| devopsBuildId      | string  |     |      |
| estimateTimeSecond | integer |     |      |
| estimateTimeValue  | string  |     |      |
| executeNum         | integer |     |      |
| executeTimeSecond  | integer |     |      |
| executeTimeValue   | string  |     |      |
| id                 | string  |     |      |
| message            | string  |     |      |
| pipelineId         | string  |     |      |
| pipelineName       | string  |     |      |
| projectId          | string  |     |      |
| startTime          | string  |     |      |
| status             | string  |     |      |
| turboPlanId        | string  |     |      |
| turboRatio         | string  |     |      |

 
