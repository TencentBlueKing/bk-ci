### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/version/rollback_draft
### 资源描述
#### 回滚到指定的历史版本并覆盖草稿
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
| version    | integer | √   | 回回滚目标版本 |

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
| default | [ResultPipelineVersionSimple](#ResultPipelineVersionSimple) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}&version={version}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "baseVersion" : 0,
    "baseVersionName" : "",
    "createTime" : 0,
    "creator" : "",
    "debugBuildId" : "",
    "description" : "",
    "latestReleasedFlag" : false,
    "pipelineId" : "",
    "pipelineVersion" : 0,
    "referCount" : 0,
    "referFlag" : false,
    "settingVersion" : 0,
    "status" : "enum",
    "triggerVersion" : 0,
    "updateTime" : 0,
    "updater" : "",
    "version" : 0,
    "versionName" : "",
    "versionNum" : 0,
    "yamlVersion" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPipelineVersionSimple
##### 数据返回包装模型

| 参数名称    | 参数类型                                            | 必须  | 参数说明 |
| ------- | ----------------------------------------------- | --- | ---- |
| data    | [PipelineVersionSimple](#PipelineVersionSimple) |     |      |
| message | string                                          |     | 错误信息 |
| status  | integer                                         | √   | 状态码  |

#### PipelineVersionSimple
##### 流水线版本摘要

| 参数名称               | 参数类型                                                                      | 必须  | 参数说明               |
| ------------------ | ------------------------------------------------------------------------- | --- | ------------------ |
| baseVersion        | integer                                                                   |     | 该版本的来源版本（空时一定为主路径） |
| baseVersionName    | string                                                                    |     | 基准版本的版本名称          |
| createTime         | integer                                                                   | √   | 创建时间戳              |
| creator            | string                                                                    | √   | 流水线创建人             |
| debugBuildId       | string                                                                    |     | 调试构建ID             |
| description        | string                                                                    |     | 版本变更说明             |
| latestReleasedFlag | boolean                                                                   |     | 当前最新正式版本标识         |
| pipelineId         | string                                                                    | √   | 流水线ID              |
| pipelineVersion    | integer                                                                   |     | 编排版本号              |
| referCount         | integer                                                                   |     | 关联构建记录总数           |
| referFlag          | boolean                                                                   |     | 是否还有构建记录引用该版本标识    |
| settingVersion     | integer                                                                   |     | 配置版本号              |
| status             | ENUM(RELEASED, COMMITTING, BRANCH, BRANCH_RELEASE, DRAFT_RELEASE, DELETE) |     | 草稿版本标识             |
| triggerVersion     | integer                                                                   |     | 触发器版本号             |
| updateTime         | integer                                                                   |     | 更新时间戳              |
| updater            | string                                                                    |     | 更新操作人              |
| version            | integer                                                                   | √   | 流水线版本号             |
| versionName        | string                                                                    | √   | 流水线版本名称            |
| versionNum         | integer                                                                   |     | 发布版本号              |
| yamlVersion        | string                                                                    |     | YAML编排版本           |

 
