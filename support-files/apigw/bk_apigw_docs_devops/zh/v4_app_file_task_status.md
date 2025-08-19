### 请求方法/请求路径
#### GET /{apigwType}/v4/artifactory/projects/{projectId}/file_task
### 资源描述
#### 查询文件托管任务状态
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
| stepId     | String |     | stepId     |
| taskId     | String |     | taskId     |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                      | 说明               |
| ------- | ----------------------------------------- | ---------------- |
| default | [ResultFileTaskInfo](#ResultFileTaskInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}&stepId={stepId}&taskId={taskId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "id" : "",
    "ip" : "",
    "path" : "",
    "status" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultFileTaskInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                          | 必须  | 参数说明 |
| ------- | ----------------------------- | --- | ---- |
| data    | [FileTaskInfo](#FileTaskInfo) |     |      |
| message | string                        |     | 错误信息 |
| status  | integer                       | √   | 状态码  |

#### FileTaskInfo
##### 版本仓库-文件托管任务信息

| 参数名称   | 参数类型    | 必须  | 参数说明     |
| ------ | ------- | --- | -------- |
| id     | string  | √   | 任务Id     |
| ip     | string  | √   | 文件所在机器IP |
| path   | string  | √   | 文件绝对路径   |
| status | integer | √   | 任务状态     |

 
