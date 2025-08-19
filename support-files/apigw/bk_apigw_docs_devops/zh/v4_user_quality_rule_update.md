### 请求方法/请求路径
#### PUT /{apigwType}/v4/projects/{projectId}/quality/rule
### 资源描述
#### 更新拦截规则列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明 |
| ---------- | ------ | --- | ---- |
| ruleHashId | String | √   | 规则ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称 | 参数类型                                    | 必须   |
| ---- | --------------------------------------- | ---- |
| 规则内容 | [RuleUpdateRequest](#RuleUpdateRequest) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X PUT '[请替换为上方API地址栏请求地址]?ruleHashId={ruleHashId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### PUT 请求样例

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
  "data" : false,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### RuleUpdateRequest
##### 规则更新请求

| 参数名称                 | 参数类型                                                             | 必须  | 参数说明         |
| -------------------- | ---------------------------------------------------------------- | --- | ------------ |
| auditTimeoutMinutes  | integer                                                          |     | 审核超时时间       |
| auditUserList        | List<string>                                                     |     | 审核通知人员       |
| controlPoint         | string                                                           | √   | 控制点          |
| controlPointPosition | string                                                           | √   | 控制点位置        |
| desc                 | string                                                           | √   | 规则描述         |
| gatewayId            | string                                                           |     | 红线匹配的id(必填)  |
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

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
