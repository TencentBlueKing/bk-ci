### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/operation_log
### 资源描述
#### 获取流水线操作日志列表（分页）
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明     |
| ---------- | ------- | --- | -------- |
| creator    | String  |     | 搜索字段：创建人 |
| page       | integer |     | 第几页      |
| pageSize   | integer |     | 每页多少条    |
| pipelineId | String  | √   | 流水线ID    |

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
| default | [ResultPagePipelineOperationDetail](#ResultPagePipelineOperationDetail) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?creator={creator}&page={page}&pageSize={pageSize}&pipelineId={pipelineId}' \
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
      "description" : "",
      "id" : 0,
      "operateTime" : 0,
      "operationLogStr" : "",
      "operationLogType" : "enum",
      "operator" : "",
      "pacRefs" : "",
      "params" : "",
      "pipelineId" : "",
      "projectId" : "",
      "status" : "enum",
      "version" : 0,
      "versionCreateTime" : 0,
      "versionName" : ""
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPagePipelineOperationDetail
##### 数据返回包装模型

| 参数名称    | 参数类型                                                        | 必须  | 参数说明 |
| ------- | ----------------------------------------------------------- | --- | ---- |
| data    | [PagePipelineOperationDetail](#PagePipelineOperationDetail) |     |      |
| message | string                                                      |     | 错误信息 |
| status  | integer                                                     | √   | 状态码  |

#### PagePipelineOperationDetail
##### 分页数据包装模型

| 参数名称       | 参数类型                                                      | 必须  | 参数说明  |
| ---------- | --------------------------------------------------------- | --- | ----- |
| count      | integer                                                   | √   | 总记录行数 |
| page       | integer                                                   | √   | 第几页   |
| pageSize   | integer                                                   | √   | 每页多少条 |
| records    | List<[PipelineOperationDetail](#PipelineOperationDetail)> | √   | 数据    |
| totalPages | integer                                                   | √   | 总共多少页 |

#### PipelineOperationDetail
##### 流水线操作日志

| 参数名称              | 参数类型                                                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明         |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------ |
| description       | string                                                                                                                                                                                                                                                                                                                                                            |     | 操作内容         |
| id                | integer                                                                                                                                                                                                                                                                                                                                                           |     | 唯一标识ID       |
| operateTime       | integer                                                                                                                                                                                                                                                                                                                                                           | √   | 操作时间         |
| operationLogStr   | string                                                                                                                                                                                                                                                                                                                                                            | √   | 操作类型文字（国际化后） |
| operationLogType  | ENUM(CREATE_PIPELINE_AND_DRAFT, CREATE_DRAFT_VERSION, UPDATE_DRAFT_VERSION, CREATE_BRANCH_VERSION, UPDATE_BRANCH_VERSION, RELEASE_MASTER_VERSION, DISABLE_PIPELINE, ENABLE_PIPELINE, ADD_PIPELINE_OWNER, ADD_PIPELINE_TO_GROUP, MOVE_PIPELINE_OUT_OF_GROUP, UPDATE_PIPELINE_SETTING, RESET_RECOMMENDED_VERSION_BUILD_NO, NORMAL_SAVE_OPERATION, PIPELINE_ARCHIVE) | √   | 操作类型         |
| operator          | string                                                                                                                                                                                                                                                                                                                                                            | √   | 操作用户         |
| pacRefs           | string                                                                                                                                                                                                                                                                                                                                                            |     | 来源代码库标识（分支名） |
| params            | string                                                                                                                                                                                                                                                                                                                                                            | √   | 操作参数         |
| pipelineId        | string                                                                                                                                                                                                                                                                                                                                                            | √   | 流水线ID        |
| projectId         | string                                                                                                                                                                                                                                                                                                                                                            | √   | 项目ID         |
| status            | ENUM(RELEASED, COMMITTING, BRANCH, BRANCH_RELEASE, DRAFT_RELEASE, DELETE)                                                                                                                                                                                                                                                                                         |     | 草稿版本标识       |
| version           | integer                                                                                                                                                                                                                                                                                                                                                           | √   | 版本ID         |
| versionCreateTime | integer                                                                                                                                                                                                                                                                                                                                                           |     | 版本创建时间       |
| versionName       | string                                                                                                                                                                                                                                                                                                                                                            |     | 版本名称         |

 
