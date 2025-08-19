### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/quality/rule
### 资源描述
#### 创建拦截规则
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称 | 参数类型                                    | 必须   |
| ---- | --------------------------------------- | ---- |
| 规则内容 | [RuleCreateRequest](#RuleCreateRequest) | true |

#### 响应参数

| HTTP代码  | 参数类型                          | 说明               |
| ------- | ----------------------------- | ---------------- |
| default | [ResultString](#ResultString) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "auditTimeoutMinutes" : 0,
  "auditUserList" : [ "" ],
  "controlPoint" : "",
  "controlPointPosition" : "",
  "desc" : "",
  "gatewayId" : "",
  "indicatorIds" : [ {
    "hashId" : "",
    "operation" : "",
    "threshold" : ""
  } ],
  "name" : "",
  "notifyGroupList" : [ "" ],
  "notifyTypeList" : [ "enum" ],
  "notifyUserList" : [ "" ],
  "operation" : "enum",
  "range" : [ "" ],
  "templateRange" : [ "" ]
}
```

### default 返回样例

```Json
{
  "data" : "",
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### RuleCreateRequest
##### 规则创建请求

| 参数名称                 | 参数类型                                                             | 必须  | 参数说明         |
| -------------------- | ---------------------------------------------------------------- | --- | ------------ |
| auditTimeoutMinutes  | integer                                                          |     | 审核超时时间       |
| auditUserList        | List<string>                                                     |     | 审核通知人员       |
| controlPoint         | string                                                           | √   | 控制点          |
| controlPointPosition | string                                                           | √   | 控制点位置        |
| desc                 | string                                                           | √   | 规则描述         |
| gatewayId            | string                                                           |     | 红线匹配的id      |
| indicatorIds         | List<[CreateRequestIndicator](#CreateRequestIndicator)>          | √   | 指标类型         |
| name                 | string                                                           | √   | 规则名称         |
| notifyGroupList      | List<string>                                                     |     | 通知组名单        |
| notifyTypeList       | List<ENUM(RTX, EMAIL, WECHAT, SMS, WEWORK, WEWORK_GROUP, VOICE)> |     | 通知类型         |
| notifyUserList       | List<string>                                                     |     | 通知人员名单       |
| operation            | ENUM(END, AUDIT)                                                 | √   | 操作类型         |
| range                | List<string>                                                     | √   | 生效的流水线id集合   |
| templateRange        | List<string>                                                     | √   | 生效的流水线模板id集合 |

#### CreateRequestIndicator
##### 指标类型

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| hashId    | string |     |      |
| operation | string |     |      |
| threshold | string |     |      |

#### ResultString
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | string  |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
