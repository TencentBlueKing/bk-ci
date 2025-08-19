### 请求方法/请求路径
#### GET /{apigwType}/v4/{projectId}/webhook/pipeline_webhook_list
### 资源描述
#### 获取流水线的webhook列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   |            |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明              |
| ---------- | ------- | --- | ----------------- |
| page       | integer |     | 页码                |
| pageSize   | integer |     | 每页条数(默认20, 最大100) |
| pipelineId | String  |     |                   |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                    | 说明               |
| ------- | ------------------------------------------------------- | ---------------- |
| default | [ResultListPipelineWebhook](#ResultListPipelineWebhook) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?page={page}&pageSize={pageSize}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "eventType" : "",
    "externalId" : "",
    "externalName" : "",
    "id" : 0,
    "pipelineId" : "",
    "projectId" : "",
    "projectName" : "",
    "repoHashId" : "",
    "repoName" : "",
    "repoType" : "enum",
    "repositoryHashId" : "",
    "repositoryType" : "enum",
    "taskId" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListPipelineWebhook
##### 数据返回包装模型

| 参数名称    | 参数类型                                      | 必须  | 参数说明 |
| ------- | ----------------------------------------- | --- | ---- |
| data    | List<[PipelineWebhook](#PipelineWebhook)> |     | 数据   |
| message | string                                    |     | 错误信息 |
| status  | integer                                   | √   | 状态码  |

#### PipelineWebhook
##### 流水线http回调模型

| 参数名称             | 参数类型                                                                                        | 必须  | 参数说明                                                         |
| ---------------- | ------------------------------------------------------------------------------------------- | --- | ------------------------------------------------------------ |
| eventType        | string                                                                                      |     | 事件类型                                                         |
| externalId       | string                                                                                      |     | 代码库平台ID                                                      |
| externalName     | string                                                                                      |     | 代码库平台仓库名                                                     |
| id               | integer                                                                                     |     | 代码库自增ID，唯一                                                   |
| pipelineId       | string                                                                                      | √   | 流水线id                                                        |
| projectId        | string                                                                                      | √   | 项目id                                                         |
| projectName      | string                                                                                      |     | 项目名称                                                         |
| repoHashId       | string                                                                                      |     | 插件配置的代码库HashId，repoHashId与repoName 不能同时为空，如果两个都不为空就用repoName |
| repoName         | string                                                                                      |     | 代码库别名                                                        |
| repoType         | ENUM(ID, NAME)                                                                              |     | 代码库标识类型， ID 代码库HashId / NAME 别名                              |
| repositoryHashId | string                                                                                      |     | 代码库hashId,插件配置解析后的代码库ID                                      |
| repositoryType   | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) | √   | 代码库类型，见ScmType枚举                                             |
| taskId           | string                                                                                      |     | 拉取当前代码库所在的插件ID                                               |

 
