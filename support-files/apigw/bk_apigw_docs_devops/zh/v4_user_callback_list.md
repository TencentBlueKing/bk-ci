### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/callbacks/callback_list
### 资源描述
#### callback回调列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | projectId  |

#### Query参数

| 参数名称     | 参数类型    | 必须  | 参数说明              |
| -------- | ------- | --- | ----------------- |
| page     | integer |     | 第几页               |
| pageSize | integer |     | 每页条数(默认20, 最大100) |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                                    | 说明               |
| ------- | ----------------------------------------------------------------------- | ---------------- |
| default | [ResultPageProjectPipelineCallBack](#ResultPageProjectPipelineCallBack) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?page={page}&pageSize={pageSize}' \
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
      "enable" : false,
      "events" : "",
      "failureTime" : "string",
      "id" : 0,
      "name" : "",
      "projectId" : "",
      "secretParam" : {
        "@type" : ""
      },
      "secretToken" : ""
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageProjectPipelineCallBack
##### 数据返回包装模型

| 参数名称    | 参数类型                                                        | 必须  | 参数说明 |
| ------- | ----------------------------------------------------------- | --- | ---- |
| data    | [PageProjectPipelineCallBack](#PageProjectPipelineCallBack) |     |      |
| message | string                                                      |     | 错误信息 |
| status  | integer                                                     | √   | 状态码  |

#### PageProjectPipelineCallBack
##### 分页数据包装模型

| 参数名称       | 参数类型                                                      | 必须  | 参数说明  |
| ---------- | --------------------------------------------------------- | --- | ----- |
| count      | integer                                                   | √   | 总记录行数 |
| page       | integer                                                   | √   | 第几页   |
| pageSize   | integer                                                   | √   | 每页多少条 |
| records    | List<[ProjectPipelineCallBack](#ProjectPipelineCallBack)> | √   | 数据    |
| totalPages | integer                                                   | √   | 总共多少页 |

#### ProjectPipelineCallBack
##### 项目的流水线回调配置

| 参数名称        | 参数类型                          | 必须  | 参数说明    |
| ----------- | ----------------------------- | --- | ------- |
| callBackUrl | string                        | √   | 回调url地址 |
| enable      | boolean                       |     | 回调是否启用  |
| events      | string                        | √   | 事件      |
| failureTime | string                        |     | 回调是否启用  |
| id          | integer                       |     | 流水线id   |
| name        | string                        |     | 回调名称    |
| projectId   | string                        | √   | 项目id    |
| secretParam | [ISecretParam](#ISecretParam) |     |         |
| secretToken | string                        |     | 密钥      |

#### ISecretParam
##### 凭证参数

| 参数名称  | 参数类型   | 必须  | 参数说明                                       |
| ----- | ------ | --- | ------------------------------------------ |
| @type | string | √   | 用于指定实现某一多态类, 可选[HeaderSecretParam],具体实现见下方 |

#### HeaderSecretParam
 *多态基类 <ISecretParam> 的实现处, 其中当字段 @type = [header] 时指定为该类实现*
 

| 参数名称    | 参数类型                | 必须  | 参数说明  |
| ------- | ------------------- | --- | ----- |
| @type   | string              | 必须是 | 多态类实现 | header |
| headers | Map<String, string> |     |       |

 
