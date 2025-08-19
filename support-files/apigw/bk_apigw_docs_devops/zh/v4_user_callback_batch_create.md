### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/callbacks/batch
### 资源描述
#### 批量创建callback回调
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | projectId  |

#### Query参数

| 参数名称        | 参数类型                   | 必须  | 参数说明        |
| ----------- | ---------------------- | --- | ----------- |
| event       | String                 | √   | event       |
| region      | ENUM(DEVNET, OSS, IDC) | √   | region      |
| secretToken | String                 |     | secretToken |
| url         | String                 | √   | url         |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                      | 说明               |
| ------- | --------------------------------------------------------- | ---------------- |
| default | [ResultCreateCallBackResult](#ResultCreateCallBackResult) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?event={event}&region={region}&secretToken={secretToken}&url={url}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "failureEvents" : {
      "string" : ""
    },
    "successEvents" : [ "" ]
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultCreateCallBackResult
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | [CreateCallBackResult](#CreateCallBackResult) |     |      |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### CreateCallBackResult
##### 项目的流水线回调创建结果

| 参数名称          | 参数类型                | 必须  | 参数说明   |
| ------------- | ------------------- | --- | ------ |
| failureEvents | Map<String, string> | √   | 失败事件列表 |
| successEvents | List<string>        | √   | 成功事件列表 |

 
