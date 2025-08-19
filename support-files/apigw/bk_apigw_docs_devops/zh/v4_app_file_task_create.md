### 请求方法/请求路径
#### POST /{apigwType}/v4/artifactory/projects/{projectId}/file_task
### 资源描述
#### 创建文件托管任务
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明      |
| --------- | ------ | --- | --------- |
| projectId | String | √   | projectId |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明       |
| ---------- | ------ | --- | ---------- |
| buildId    | String | √   | buildId    |
| pipelineId | String |     | pipelineId |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称   | 参数类型                                    | 必须   |
| ------ | --------------------------------------- | ---- |
| taskId | [CreateFileTaskReq](#CreateFileTaskReq) | true |

#### 响应参数

| HTTP代码  | 参数类型                          | 说明               |
| ------- | ----------------------------- | ---------------- |
| default | [ResultString](#ResultString) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "fileType" : "enum",
  "path" : ""
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
#### CreateFileTaskReq
##### 创建文件托管任务请求

| 参数名称     | 参数类型                                                            | 必须  | 参数说明 |
| -------- | --------------------------------------------------------------- | --- | ---- |
| fileType | ENUM(BK_ARCHIVE, BK_CUSTOM, BK_REPORT, BK_PLUGIN_FE, BK_STATIC) | √   | 文件类型 |
| path     | string                                                          | √   | 文件路径 |

#### ResultString
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | string  |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
