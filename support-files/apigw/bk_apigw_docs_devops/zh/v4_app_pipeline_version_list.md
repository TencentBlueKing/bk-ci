### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/version_list
### 资源描述
#### 流水线编排版本列表（搜索、分页）
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称        | 参数类型    | 必须  | 参数说明         |
| ----------- | ------- | --- | ------------ |
| creator     | String  |     | 搜索字段：创建人     |
| description | String  |     | 搜索字段：变更说明    |
| fromVersion | integer |     | 跳转定位的版本号     |
| page        | integer |     | 第几页          |
| pageSize    | integer |     | 每页多少条        |
| pipelineId  | String  | √   | 流水线ID        |
| versionName | String  |     | 搜索字段：版本名包含字符 |

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
| default | [ResultPagePipelineVersionWithInfo](#ResultPagePipelineVersionWithInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?creator={creator}&description={description}&fromVersion={fromVersion}&page={page}&pageSize={pageSize}&pipelineId={pipelineId}&versionName={versionName}' \
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
      "baseVersion" : 0,
      "baseVersionName" : "",
      "canElementSkip" : false,
      "canManualStartup" : false,
      "channelCode" : "enum",
      "createTime" : 0,
      "creator" : "",
      "debugBuildId" : "",
      "description" : "",
      "id" : 0,
      "lastModifyUser" : "",
      "pipelineDesc" : "",
      "pipelineId" : "",
      "pipelineName" : "",
      "pipelineVersion" : 0,
      "projectId" : "",
      "referCount" : 0,
      "settingVersion" : 0,
      "status" : "enum",
      "taskCount" : 0,
      "templateId" : "",
      "triggerVersion" : 0,
      "updateTime" : 0,
      "version" : 0,
      "versionName" : "",
      "versionNum" : 0,
      "viewNames" : [ "" ]
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPagePipelineVersionWithInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                                                        | 必须  | 参数说明 |
| ------- | ----------------------------------------------------------- | --- | ---- |
| data    | [PagePipelineVersionWithInfo](#PagePipelineVersionWithInfo) |     |      |
| message | string                                                      |     | 错误信息 |
| status  | integer                                                     | √   | 状态码  |

#### PagePipelineVersionWithInfo
##### 分页数据包装模型

| 参数名称       | 参数类型                                                      | 必须  | 参数说明  |
| ---------- | --------------------------------------------------------- | --- | ----- |
| count      | integer                                                   | √   | 总记录行数 |
| page       | integer                                                   | √   | 第几页   |
| pageSize   | integer                                                   | √   | 每页多少条 |
| records    | List<[PipelineVersionWithInfo](#PipelineVersionWithInfo)> | √   | 数据    |
| totalPages | integer                                                   | √   | 总共多少页 |

#### PipelineVersionWithInfo
##### 流水线信息

| 参数名称             | 参数类型                                                                      | 必须  | 参数说明                |
| ---------------- | ------------------------------------------------------------------------- | --- | ------------------- |
| baseVersion      | integer                                                                   |     | 该版本的来源版本（空时一定为主路径）  |
| baseVersionName  | string                                                                    |     | 草稿的来源版本名称（只在草稿版本有值） |
| canElementSkip   | boolean                                                                   | √   | 是否可以跳过              |
| canManualStartup | boolean                                                                   | √   | 是否能够手动启动            |
| channelCode      | ENUM(BS, AM, CODECC, GCLOUD, GIT, GONGFENGSCAN, CODECC_EE)                | √   | 渠道代码                |
| createTime       | integer                                                                   | √   | 创建时间                |
| creator          | string                                                                    | √   | 版本创建者               |
| debugBuildId     | string                                                                    |     | 调试构建ID              |
| description      | string                                                                    |     | 版本变更说明              |
| id               | integer                                                                   |     | ID                  |
| lastModifyUser   | string                                                                    | √   | 上一次的更新者             |
| pipelineDesc     | string                                                                    | √   | 流水线描述               |
| pipelineId       | string                                                                    | √   | 流水线DI               |
| pipelineName     | string                                                                    | √   | 流水线名称               |
| pipelineVersion  | integer                                                                   |     | 编排版本号               |
| projectId        | string                                                                    | √   | 项目ID                |
| referCount       | integer                                                                   |     | 关联构建记录总数            |
| settingVersion   | integer                                                                   |     | 配置版本号               |
| status           | ENUM(RELEASED, COMMITTING, BRANCH, BRANCH_RELEASE, DRAFT_RELEASE, DELETE) |     | 草稿版本状态标识            |
| taskCount        | integer                                                                   | √   | 任务数                 |
| templateId       | string                                                                    |     | 模板ID                |
| triggerVersion   | integer                                                                   |     | 触发器版本号              |
| updateTime       | integer                                                                   |     | 更新时间                |
| version          | integer                                                                   | √   | 版本                  |
| versionName      | string                                                                    | √   | 版本名称                |
| versionNum       | integer                                                                   |     | 发布版本号               |
| viewNames        | List<string>                                                              |     | 流水线组名称列表            |

 
