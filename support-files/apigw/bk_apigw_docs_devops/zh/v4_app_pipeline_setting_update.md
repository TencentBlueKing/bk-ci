### 请求方法/请求路径
#### PUT /{apigwType}/v4/projects/{projectId}/pipelines/setting
### 资源描述
#### 更新流水线设置
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

| 参数名称  | 参数类型                                | 必须   |
| ----- | ----------------------------------- | ---- |
| 流水线设置 | [PipelineSetting](#PipelineSetting) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X PUT '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### PUT 请求样例

```Json
{
  "buildNumRule" : "",
  "cleanVariablesWhenRetry" : false,
  "concurrencyCancelInProgress" : false,
  "concurrencyGroup" : "",
  "desc" : "",
  "failIfVariableInvalid" : false,
  "failSubscription" : {
    "content" : "",
    "detailFlag" : false,
    "groups" : [ "" ],
    "types" : [ "enum" ],
    "users" : "",
    "wechatGroup" : "",
    "wechatGroupFlag" : false,
    "wechatGroupMarkdownFlag" : false
  },
  "failSubscriptionList" : [ {
    "content" : "",
    "detailFlag" : false,
    "groups" : [ "" ],
    "types" : [ "enum" ],
    "users" : "",
    "wechatGroup" : "",
    "wechatGroupFlag" : false,
    "wechatGroupMarkdownFlag" : false
  } ],
  "labelNames" : [ "" ],
  "labels" : [ "" ],
  "maxConRunningQueueSize" : 0,
  "maxPipelineResNum" : 0,
  "maxQueueSize" : 0,
  "pipelineAsCodeSettings" : {
    "enable" : false,
    "inheritedDialect" : false,
    "pipelineDialect" : "",
    "projectDialect" : ""
  },
  "pipelineId" : "",
  "pipelineName" : "",
  "projectId" : "",
  "runLockType" : "enum",
  "successSubscription" : {
    "content" : "",
    "detailFlag" : false,
    "groups" : [ "" ],
    "types" : [ "enum" ],
    "users" : "",
    "wechatGroup" : "",
    "wechatGroupFlag" : false,
    "wechatGroupMarkdownFlag" : false
  },
  "successSubscriptionList" : [ {
    "content" : "",
    "detailFlag" : false,
    "groups" : [ "" ],
    "types" : [ "enum" ],
    "users" : "",
    "wechatGroup" : "",
    "wechatGroupFlag" : false,
    "wechatGroupMarkdownFlag" : false
  } ],
  "version" : 0,
  "waitQueueTimeMinute" : 0
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
#### PipelineSetting
##### 流水线配置

| 参数名称                        | 参数类型                                                  | 必须  | 参数说明                    |
| --------------------------- | ----------------------------------------------------- | --- | ----------------------- |
| buildNumRule                | string                                                |     | 构建号生成规则                 |
| cleanVariablesWhenRetry     | boolean                                               |     | 重试时清理引擎变量表              |
| concurrencyCancelInProgress | boolean                                               | √   | 并发时,是否相同group取消正在执行的流水线 |
| concurrencyGroup            | string                                                |     | 并发时,设定的group            |
| desc                        | string                                                | √   | 描述                      |
| failIfVariableInvalid       | boolean                                               |     | 是否配置流水线变量值超长时终止执行       |
| failSubscription            | [Subscription](#Subscription)                         |     |                         |
| failSubscriptionList        | List<[Subscription](#Subscription)>                   |     | 订阅失败通知组                 |
| labelNames                  | List<string>                                          | √   | 标签名称列表（仅用于前端展示，不参与数据保存） |
| labels                      | List<string>                                          | √   | 标签ID列表                  |
| maxConRunningQueueSize      | integer                                               |     | 并发构建数量限制                |
| maxPipelineResNum           | integer                                               | √   | 保存流水线编排的最大个数            |
| maxQueueSize                | integer                                               | √   | 最大排队数量                  |
| pipelineAsCodeSettings      | [PipelineAsCodeSettings](#PipelineAsCodeSettings)     |     |                         |
| pipelineId                  | string                                                | √   | 该字段只读流水线id              |
| pipelineName                | string                                                | √   | 流水线名称                   |
| projectId                   | string                                                | √   | 该字段只读项目id               |
| runLockType                 | ENUM(MULTIPLE, SINGLE, SINGLE_LOCK, LOCK, GROUP_LOCK) | √   | 流水线运行锁定方式               |
| successSubscription         | [Subscription](#Subscription)                         |     |                         |
| successSubscriptionList     | List<[Subscription](#Subscription)>                   |     | 订阅成功通知组                 |
| version                     | integer                                               | √   | 版本                      |
| waitQueueTimeMinute         | integer                                               | √   | 最大排队时长                  |

#### Subscription
##### 设置-订阅消息

| 参数名称                    | 参数类型                                                             | 必须  | 参数说明                  |
| ----------------------- | ---------------------------------------------------------------- | --- | --------------------- |
| content                 | string                                                           | √   | 自定义通知内容               |
| detailFlag              | boolean                                                          | √   | 通知的流水线详情连接开关          |
| groups                  | List<string>                                                     | √   | 分组                    |
| types                   | List<ENUM(EMAIL, RTX, WECHAT, SMS, WEWORK, VOICE, WEWORK_GROUP)> | √   | 通知方式(email, rtx)      |
| users                   | string                                                           | √   | 通知人员                  |
| wechatGroup             | string                                                           | √   | 企业微信群通知群ID            |
| wechatGroupFlag         | boolean                                                          | √   | 企业微信群通知开关             |
| wechatGroupMarkdownFlag | boolean                                                          | √   | 企业微信群通知转为Markdown格式开关 |

#### PipelineAsCodeSettings
##### 设置-YAML流水线功能设置

| 参数名称             | 参数类型    | 必须  | 参数说明          |
| ---------------- | ------- | --- | ------------- |
| enable           | boolean | √   | 是否支持YAML流水线功能 |
| inheritedDialect | boolean |     | 是否继承项目流水线语言风格 |
| pipelineDialect  | string  |     | 流水线语言风格       |
| projectDialect   | string  |     | 项目级流水线语法风格    |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
