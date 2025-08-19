### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/pipelines/paging_search_by_name
### 资源描述
#### 根据流水线名称搜索
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称         | 参数类型    | 必须  | 参数说明              |
| ------------ | ------- | --- | ----------------- |
| page         | integer |     | 第几页               |
| pageSize     | integer |     | 每页条数(默认20, 最大100) |
| pipelineName | String  |     | 搜索名称              |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                                                      | 说明               |
| ------- | ----------------------------------------------------------------------------------------- | ---------------- |
| default | [ResultPipelineViewPipelinePagePipelineInfo](#ResultPipelineViewPipelinePagePipelineInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?page={page}&pageSize={pageSize}&pipelineName={pipelineName}' \
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
      "canElementSkip" : false,
      "canManualStartup" : false,
      "channelCode" : "enum",
      "createTime" : 0,
      "creator" : "",
      "id" : 0,
      "lastBuildMsg" : "",
      "lastModifyUser" : "",
      "latestBuildEndTime" : 0,
      "latestBuildId" : "",
      "latestBuildNum" : 0,
      "latestBuildStartTime" : 0,
      "latestBuildStatus" : "enum",
      "latestVersionStatus" : "enum",
      "locked" : false,
      "permissions" : {
        "canArchive" : false,
        "canDelete" : false,
        "canDownload" : false,
        "canEdit" : false,
        "canExecute" : false,
        "canManage" : false,
        "canShare" : false,
        "canView" : false
      },
      "pipelineDesc" : "",
      "pipelineId" : "",
      "pipelineName" : "",
      "projectId" : "",
      "taskCount" : 0,
      "templateId" : "",
      "templateInfo" : {
        "desc" : "",
        "instanceType" : "enum",
        "templateId" : "",
        "templateName" : "",
        "version" : 0,
        "versionName" : ""
      },
      "trigger" : "",
      "updateTime" : 0,
      "version" : 0,
      "versionName" : "",
      "viewNames" : [ "" ]
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPipelineViewPipelinePagePipelineInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                                                                          | 必须  | 参数说明 |
| ------- | ----------------------------------------------------------------------------- | --- | ---- |
| data    | [PipelineViewPipelinePagePipelineInfo](#PipelineViewPipelinePagePipelineInfo) |     |      |
| message | string                                                                        |     | 错误信息 |
| status  | integer                                                                       | √   | 状态码  |

#### PipelineViewPipelinePagePipelineInfo
##### Pipeline分页数据包装模型

| 参数名称       | 参数类型                                | 必须  | 参数说明  |
| ---------- | ----------------------------------- | --- | ----- |
| count      | integer                             | √   | 总记录行数 |
| page       | integer                             | √   | 第几页   |
| pageSize   | integer                             | √   | 每页多少条 |
| records    | List<[PipelineInfo](#PipelineInfo)> | √   | 数据    |
| totalPages | integer                             | √   | 总共多少页 |

#### PipelineInfo
##### 流水线信息

| 参数名称                 | 参数类型                                                                                                                                                                                                                                                                                                                                                                                         | 必须  | 参数说明                      |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------------------- |
| canElementSkip       | boolean                                                                                                                                                                                                                                                                                                                                                                                      |     | 是否可以跳过                    |
| canManualStartup     | boolean                                                                                                                                                                                                                                                                                                                                                                                      |     | 是否能够手动启动                  |
| channelCode          | ENUM(BS, AM, CODECC, GCLOUD, GIT, GONGFENGSCAN, CODECC_EE)                                                                                                                                                                                                                                                                                                                                   |     | 渠道代码                      |
| createTime           | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 创建时间                      |
| creator              | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 创建者                       |
| id                   | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | ID                        |
| lastBuildMsg         | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 最后一次构建的构建信息               |
| lastModifyUser       | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 上一次的更新者                   |
| latestBuildEndTime   | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 最后构建结束时间                  |
| latestBuildId        | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 最后构建ID                    |
| latestBuildNum       | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 最后构建版本号                   |
| latestBuildStartTime | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 最后构建启动时间                  |
| latestBuildStatus    | ENUM(SUCCEED, FAILED, CANCELED, RUNNING, TERMINATE, REVIEWING, REVIEW_ABORT, REVIEW_PROCESSED, HEARTBEAT_TIMEOUT, PREPARE_ENV, UNEXEC, SKIP, QUALITY_CHECK_FAIL, QUEUE, LOOP_WAITING, CALL_WAITING, TRY_FINALLY, QUEUE_TIMEOUT, EXEC_TIMEOUT, QUEUE_CACHE, RETRY, PAUSE, STAGE_SUCCESS, QUOTA_FAILED, DEPENDENT_WAITING, QUALITY_CHECK_PASS, QUALITY_CHECK_WAIT, TRIGGER_REVIEWING, UNKNOWN) |     | 最后构建状态                    |
| latestVersionStatus  | ENUM(RELEASED, COMMITTING, BRANCH, BRANCH_RELEASE, DRAFT_RELEASE, DELETE)                                                                                                                                                                                                                                                                                                                    |     | 最新流水线版本状态（如有任何发布版本则为发布版本） |
| locked               | boolean                                                                                                                                                                                                                                                                                                                                                                                      |     | 流水线被锁定，即禁用                |
| permissions          | [PipelinePermissions](#PipelinePermissions)                                                                                                                                                                                                                                                                                                                                                  |     |                           |
| pipelineDesc         | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 流水线描述                     |
| pipelineId           | string                                                                                                                                                                                                                                                                                                                                                                                       | √   | 流水线DI                     |
| pipelineName         | string                                                                                                                                                                                                                                                                                                                                                                                       | √   | 流水线名称                     |
| projectId            | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 项目ID                      |
| taskCount            | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 任务数                       |
| templateId           | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 模板ID                      |
| templateInfo         | [TemplateInfo](#TemplateInfo)                                                                                                                                                                                                                                                                                                                                                                |     |                           |
| trigger              | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 触发方式                      |
| updateTime           | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 更新时间                      |
| version              | integer                                                                                                                                                                                                                                                                                                                                                                                      |     | 版本                        |
| versionName          | string                                                                                                                                                                                                                                                                                                                                                                                       |     | 版本名称                      |
| viewNames            | List<string>                                                                                                                                                                                                                                                                                                                                                                                 |     | 流水线组名称列表                  |

#### PipelinePermissions
##### 流水线-流水线权限

| 参数名称        | 参数类型    | 必须  | 参数说明  |
| ----------- | ------- | --- | ----- |
| canArchive  | boolean | √   | 归档权限  |
| canDelete   | boolean | √   | 删除权限  |
| canDownload | boolean | √   | 下载权限  |
| canEdit     | boolean | √   | 编辑权限  |
| canExecute  | boolean | √   | 执行权限  |
| canManage   | boolean | √   | 管理员权限 |
| canShare    | boolean | √   | 分享权限  |
| canView     | boolean | √   | 查看权限  |

#### TemplateInfo
##### 子流水线参数键值对

| 参数名称         | 参数类型                      | 必须  | 参数说明 |
| ------------ | ------------------------- | --- | ---- |
| desc         | string                    |     | 版本描述 |
| instanceType | ENUM(FREEDOM, CONSTRAINT) | √   | 关联模式 |
| templateId   | string                    | √   | 模板ID |
| templateName | string                    | √   | 模板名称 |
| version      | integer                   | √   | 版本号  |
| versionName  | string                    | √   | 版本名称 |

 
