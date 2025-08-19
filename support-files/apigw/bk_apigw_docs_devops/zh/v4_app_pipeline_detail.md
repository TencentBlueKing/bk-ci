### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/pipeline_detail
### 资源描述
#### 获取流水线信息（含草稿）
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明  |
| ---------- | ------ | --- | ----- |
| pipelineId | String | √   | 流水线ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                          | 说明               |
| ------- | --------------------------------------------- | ---------------- |
| default | [ResultPipelineDetail](#ResultPipelineDetail) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "baseVersion" : 0,
    "baseVersionName" : "",
    "baseVersionStatus" : "enum",
    "canDebug" : false,
    "canManualStartup" : false,
    "canRelease" : false,
    "createTime" : 0,
    "creator" : "",
    "hasCollect" : false,
    "hasPermission" : false,
    "instanceFromTemplate" : false,
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
    "pipelineAsCodeSettings" : {
      "enable" : false,
      "inheritedDialect" : false,
      "pipelineDialect" : "",
      "projectDialect" : ""
    },
    "pipelineDesc" : "",
    "pipelineId" : "",
    "pipelineName" : "",
    "releaseVersion" : 0,
    "releaseVersionName" : "",
    "runLockType" : "enum",
    "templateId" : "",
    "templateVersion" : 0,
    "updateTime" : 0,
    "version" : 0,
    "versionName" : "",
    "viewNames" : [ "" ],
    "yamlExist" : false,
    "yamlInfo" : {
      "filePath" : "",
      "fileUrl" : "",
      "pathWithNamespace" : "",
      "repoHashId" : "",
      "scmType" : "enum",
      "status" : "",
      "webUrl" : ""
    }
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPipelineDetail
##### 数据返回包装模型

| 参数名称    | 参数类型                              | 必须  | 参数说明 |
| ------- | --------------------------------- | --- | ---- |
| data    | [PipelineDetail](#PipelineDetail) |     |      |
| message | string                            |     | 错误信息 |
| status  | integer                           | √   | 状态码  |

#### PipelineDetail
##### 流水线预览页完整信息

| 参数名称                   | 参数类型                                                                      | 必须  | 参数说明                      |
| ---------------------- | ------------------------------------------------------------------------- | --- | ------------------------- |
| baseVersion            | integer                                                                   |     | 草稿的基准版本（存在草稿才有值）          |
| baseVersionName        | string                                                                    |     | 基准版本的版本名称                 |
| baseVersionStatus      | ENUM(RELEASED, COMMITTING, BRANCH, BRANCH_RELEASE, DRAFT_RELEASE, DELETE) |     | 草稿的基准版本的状态（存在草稿才有值）       |
| canDebug               | boolean                                                                   | √   | 是否可以调试                    |
| canManualStartup       | boolean                                                                   | √   | 是否可以手动触发                  |
| canRelease             | boolean                                                                   | √   | 是否可以发布                    |
| createTime             | integer                                                                   | √   | 创建时间                      |
| creator                | string                                                                    | √   | 创建者                       |
| hasCollect             | boolean                                                                   | √   | 是否收藏                      |
| hasPermission          | boolean                                                                   | √   | 是否有编辑权限                   |
| instanceFromTemplate   | boolean                                                                   | √   | 是否从模板实例化                  |
| latestVersionStatus    | ENUM(RELEASED, COMMITTING, BRANCH, BRANCH_RELEASE, DRAFT_RELEASE, DELETE) |     | 最新流水线版本状态（如有任何发布版本则为发布版本） |
| locked                 | boolean                                                                   | √   | 运行锁定                      |
| permissions            | [PipelinePermissions](#PipelinePermissions)                               |     |                           |
| pipelineAsCodeSettings | [PipelineAsCodeSettings](#PipelineAsCodeSettings)                         |     |                           |
| pipelineDesc           | string                                                                    | √   | 流水线描述                     |
| pipelineId             | string                                                                    | √   | 流水线Id                     |
| pipelineName           | string                                                                    | √   | 流水线名称                     |
| releaseVersion         | integer                                                                   |     | 最新的发布版本，如果为空则说明没有过发布版本    |
| releaseVersionName     | string                                                                    |     | 最新的发布版本名称，如果为空则说明没有过发布版本  |
| runLockType            | ENUM(MULTIPLE, SINGLE, SINGLE_LOCK, LOCK, GROUP_LOCK)                     |     | 流水线运行锁定方式                 |
| templateId             | string                                                                    |     | 当前模板的ID                   |
| templateVersion        | integer                                                                   |     | 关联模板版本                    |
| updateTime             | integer                                                                   | √   | 更新时间                      |
| version                | integer                                                                   | √   | 草稿或最新的发布版本                |
| versionName            | string                                                                    |     | 草稿或最新的发布版本名称              |
| viewNames              | List<string>                                                              |     | 流水线组名称列表                  |
| yamlExist              | boolean                                                                   |     | yaml文件在默认分支是否存在           |
| yamlInfo               | [PipelineYamlVo](#PipelineYamlVo)                                         |     |                           |

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

#### PipelineAsCodeSettings
##### 设置-YAML流水线功能设置

| 参数名称             | 参数类型    | 必须  | 参数说明          |
| ---------------- | ------- | --- | ------------- |
| enable           | boolean | √   | 是否支持YAML流水线功能 |
| inheritedDialect | boolean |     | 是否继承项目流水线语言风格 |
| pipelineDialect  | string  |     | 流水线语言风格       |
| projectDialect   | string  |     | 项目级流水线语法风格    |

#### PipelineYamlVo
##### 流水线yaml展示信息

| 参数名称              | 参数类型                                                                                        | 必须  | 参数说明      |
| ----------------- | ------------------------------------------------------------------------------------------- | --- | --------- |
| filePath          | string                                                                                      | √   | yaml文件路径  |
| fileUrl           | string                                                                                      |     | yaml文件url |
| pathWithNamespace | string                                                                                      |     | 代码库项目路径   |
| repoHashId        | string                                                                                      | √   | 代码库hashId |
| scmType           | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     | 代码库类型     |
| status            | string                                                                                      |     | yaml文件状态  |
| webUrl            | string                                                                                      |     | 仓库网页url   |

 
