### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/build_start
### 资源描述
#### 启动构建
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明       |
| ---------- | ------- | --- | ---------- |
| buildNo    | integer |     | 手动指定构建版本参数 |
| pipelineId | String  | √   | 流水线ID      |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称                              | 参数类型                | 必须  |
| --------------------------------- | ------------------- | --- |
| 启动参数：map<变量名(string),变量值(string)> | Map<String, string> |     |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBuildId](#ResultBuildId) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?buildNo={buildNo}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{ }
```

### default 返回样例

```Json
{
  "data" : {
    "executeCount" : 0,
    "id" : "",
    "num" : 0,
    "pipelineId" : "",
    "projectId" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultBuildId
##### 数据返回包装模型

| 参数名称    | 参数类型                | 必须  | 参数说明 |
| ------- | ------------------- | --- | ---- |
| data    | [BuildId](#BuildId) |     |      |
| message | string              |     | 错误信息 |
| status  | integer             | √   | 状态码  |

#### BuildId
##### 构建模型-ID

| 参数名称         | 参数类型    | 必须  | 参数说明   |
| ------------ | ------- | --- | ------ |
| executeCount | integer | √   | 当前执行次数 |
| id           | string  | √   | 构建ID   |
| num          | integer |     | 构建编号   |
| pipelineId   | string  |     | 流水线ID  |
| projectId    | string  |     | 项目ID   |

 
