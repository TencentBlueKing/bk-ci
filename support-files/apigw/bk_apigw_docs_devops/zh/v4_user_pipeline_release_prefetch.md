### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/release_prefetch
### 资源描述
#### 将当前草稿发布为正式版本
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明    |
| ---------- | ------- | --- | ------- |
| pipelineId | String  | √   | 流水线ID   |
| version    | integer | √   | 流水线编排版本 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                        | 说明               |
| ------- | ----------------------------------------------------------- | ---------------- |
| default | [ResultPrefetchReleaseResult](#ResultPrefetchReleaseResult) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}&version={version}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "newVersionName" : "",
    "newVersionNum" : 0,
    "pipelineId" : "",
    "pipelineName" : "",
    "version" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPrefetchReleaseResult
##### 数据返回包装模型

| 参数名称    | 参数类型                                            | 必须  | 参数说明 |
| ------- | ----------------------------------------------- | --- | ---- |
| data    | [PrefetchReleaseResult](#PrefetchReleaseResult) |     |      |
| message | string                                          |     | 错误信息 |
| status  | integer                                         | √   | 状态码  |

#### PrefetchReleaseResult
##### 配置流水线结果

| 参数名称           | 参数类型    | 必须  | 参数说明   |
| -------------- | ------- | --- | ------ |
| newVersionName | string  | √   | 生成版本名称 |
| newVersionNum  | integer | √   | 发布版本号  |
| pipelineId     | string  | √   | 流水线ID  |
| pipelineName   | string  | √   | 流水线名称  |
| version        | integer | √   | 草稿版本号  |

 
