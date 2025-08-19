### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/callbacks
### 资源描述
#### 创建callback回调，调用需要项目管理员身份
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | projectId  |

#### Query参数

| 参数名称        | 参数类型                                                                                                                                                                                                                                                                                                       | 必须  | 参数说明                              |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | --------------------------------- |
| event       | ENUM(DELETE_PIPELINE, CREATE_PIPELINE, UPDATE_PIPELINE, STREAM_ENABLED, RESTORE_PIPELINE, BUILD_START, BUILD_END, BUILD_STAGE_START, BUILD_STAGE_END, BUILD_JOB_START, BUILD_JOB_END, BUILD_TASK_START, BUILD_TASK_END, BUILD_TASK_PAUSE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_ENABLE, PROJECT_DISABLE) | √   | event                             |
| region      | ENUM(DEVNET, OSS, IDC)                                                                                                                                                                                                                                                                                     | √   | region                            |
| secretToken | String                                                                                                                                                                                                                                                                                                     |     | 该参数将会在回调中X-DEVOPS-WEBHOOK-TOKEN返回 |
| url         | String                                                                                                                                                                                                                                                                                                     | √   | url                               |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?event={event}&region={region}&secretToken={secretToken}&url={url}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
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
#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
