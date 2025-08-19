### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/build_status
### 资源描述
#### 查看构建状态信息,#4295增加stageStatus等
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明  |
| ---------- | ------ | --- | ----- |
| buildId    | String | √   | 构建ID  |
| pipelineId | String |     | 流水线ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                      | 说明               |
| ------- | --------------------------------------------------------- | ---------------- |
| default | [ResultBuildHistoryWithVars](#ResultBuildHistoryWithVars) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "artifactList" : [ {
      "appVersion" : "",
      "artifactoryType" : "enum",
      "downloadUrl" : "",
      "folder" : false,
      "fullName" : "",
      "fullPath" : "",
      "md5" : "",
      "modifiedTime" : 0,
      "name" : "",
      "path" : "",
      "properties" : [ {
        "key" : "",
        "value" : ""
      } ],
      "registry" : "",
      "shortUrl" : "",
      "size" : 0
    } ],
    "buildMsg" : "",
    "buildNum" : 0,
    "buildNumAlias" : "",
    "buildParameters" : [ {
      "defaultValue" : "Any 任意类型，参照实际请求或返回",
      "desc" : "",
      "key" : "",
      "latestRandomStringInPath" : "",
      "readOnly" : false,
      "value" : "Any 任意类型，参照实际请求或返回",
      "valueType" : "enum"
    } ],
    "currentQueuePosition" : 0,
    "currentTimestamp" : 0,
    "endTime" : 0,
    "errorInfoList" : [ {
      "atomCode" : "",
      "containerId" : "",
      "errorCode" : 0,
      "errorMsg" : "",
      "errorType" : 0,
      "matrixFlag" : false,
      "stageId" : "",
      "taskId" : "",
      "taskName" : ""
    } ],
    "executeTime" : 0,
    "id" : "",
    "material" : [ {
      "aliasName" : "",
      "branchName" : "",
      "commitTimes" : 0,
      "createTime" : 0,
      "mainRepo" : false,
      "newCommitComment" : "",
      "newCommitId" : "",
      "scmType" : "",
      "taskId" : "",
      "url" : ""
    } ],
    "mobileStart" : false,
    "pipelineVersion" : 0,
    "queueTime" : 0,
    "recommendVersion" : "",
    "remark" : "",
    "retry" : false,
    "stageStatus" : [ {
      "elapsed" : 0,
      "name" : "",
      "showMsg" : "",
      "stageId" : "",
      "startEpoch" : 0,
      "status" : "",
      "tag" : [ "" ],
      "timeCost" : {
        "executeCost" : 0,
        "queueCost" : 0,
        "systemCost" : 0,
        "totalCost" : 0,
        "waitCost" : 0
      }
    } ],
    "startTime" : 0,
    "startType" : "",
    "status" : "",
    "totalTime" : 0,
    "trigger" : "",
    "userId" : "",
    "variables" : {
      "string" : ""
    },
    "webHookType" : "",
    "webhookInfo" : {
      "codeType" : "",
      "issueIid" : "",
      "linkUrl" : "",
      "materialId" : "",
      "materialName" : "",
      "mrId" : "",
      "mrIid" : "",
      "mrUrl" : "",
      "nameWithNamespace" : "",
      "noteId" : "",
      "parentBuildId" : "",
      "parentBuildNum" : "",
      "parentPipelineId" : "",
      "parentPipelineName" : "",
      "parentProjectId" : "",
      "refId" : "",
      "repoAuthUser" : "",
      "reviewId" : "",
      "tagName" : "",
      "webhookAliasName" : "",
      "webhookBranch" : "",
      "webhookCommitId" : "",
      "webhookEventType" : "",
      "webhookMergeCommitSha" : "",
      "webhookMessage" : "",
      "webhookRepoUrl" : "",
      "webhookSourceBranch" : "",
      "webhookType" : ""
    }
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultBuildHistoryWithVars
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | [BuildHistoryWithVars](#BuildHistoryWithVars) |     |      |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### BuildHistoryWithVars
##### 带构建变量的历史构建模型

| 参数名称                 | 参数类型                                                  | 必须  | 参数说明               |
| -------------------- | ----------------------------------------------------- | --- | ------------------ |
| artifactList         | List<[FileInfo](#FileInfo)>                           |     | 构件列表               |
| buildMsg             | string                                                |     | 构建信息               |
| buildNum             | integer                                               |     | 构建号                |
| buildNumAlias        | string                                                |     | 自定义构建版本号           |
| buildParameters      | List<[BuildParameters](#BuildParameters)>             |     | 启动参数               |
| currentQueuePosition | integer                                               | √   | 排队位置               |
| currentTimestamp     | integer                                               | √   | 服务器当前时间戳           |
| endTime              | integer                                               |     | 结束时间               |
| errorInfoList        | List<[ErrorInfo](#ErrorInfo)>                         |     | 流水线任务执行错误          |
| executeTime          | integer                                               |     | 运行耗时(毫秒，不包括人工审核时间) |
| id                   | string                                                | √   | 构建ID               |
| material             | List<[PipelineBuildMaterial](#PipelineBuildMaterial)> |     | 原材料                |
| mobileStart          | boolean                                               |     | 是否是手机启动            |
| pipelineVersion      | integer                                               | √   | 编排文件版本号            |
| queueTime            | integer                                               |     | 排队于                |
| recommendVersion     | string                                                |     | 推荐版本号              |
| remark               | string                                                |     | 备注                 |
| retry                | boolean                                               | √   | 是否重试               |
| stageStatus          | List<[BuildStageStatus](#BuildStageStatus)>           |     | 各阶段状态              |
| startTime            | integer                                               | √   | 开始时间               |
| startType            | string                                                |     | 启动类型(新)            |
| status               | string                                                | √   | 状态                 |
| totalTime            | integer                                               |     | 总耗时(毫秒)            |
| trigger              | string                                                | √   | 触发条件               |
| userId               | string                                                | √   | 启动用户               |
| variables            | Map<String, string>                                   | √   | 构建变量集合(30天左右过期删除)  |
| webHookType          | string                                                |     | WebHook类型          |
| webhookInfo          | [WebhookInfo](#WebhookInfo)                           |     |                    |

#### FileInfo
##### 版本仓库-文件信息

| 参数名称            | 参数类型                                      | 必须  | 参数说明            |
| --------------- | ----------------------------------------- | --- | --------------- |
| appVersion      | string                                    |     | app版本           |
| artifactoryType | ENUM(PIPELINE, CUSTOM_DIR, IMAGE, REPORT) | √   | 仓库类型            |
| downloadUrl     | string                                    |     | 下载链接            |
| folder          | boolean                                   | √   | 是否文件夹           |
| fullName        | string                                    | √   | 文件全名            |
| fullPath        | string                                    | √   | 文件全路径           |
| md5             | string                                    |     | MD5             |
| modifiedTime    | integer                                   | √   | 更新时间            |
| name            | string                                    | √   | 文件名             |
| path            | string                                    | √   | 文件路径            |
| properties      | List<[Property](#Property)>               |     | 元数据             |
| registry        | string                                    |     | docker registry |
| shortUrl        | string                                    |     | 下载短链接           |
| size            | integer                                   | √   | 文件大小(byte)      |

#### Property
##### 版本仓库-元数据

| 参数名称  | 参数类型   | 必须  | 参数说明 |
| ----- | ------ | --- | ---- |
| key   | string | √   | 元数据键 |
| value | string | √   | 元数据值 |

#### BuildParameters
##### 构建模型-构建参数

| 参数名称                     | 参数类型                                                                                                                                                                                            | 必须  | 参数说明                     |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------------------ |
| defaultValue             | Any                                                                                                                                                                                             |     | 默认值                      |
| desc                     | string                                                                                                                                                                                          |     | 描述                       |
| key                      | string                                                                                                                                                                                          | √   | 元素值ID-标识符                |
| latestRandomStringInPath | string                                                                                                                                                                                          |     | 目录随机字符串（仅供CUSTOM_FILE类型） |
| readOnly                 | boolean                                                                                                                                                                                         |     | 是否只读                     |
| value                    | Any                                                                                                                                                                                             | √   | 元素值名称-显示用                |
| valueType                | ENUM(string, textarea, enum, date, long, boolean, svn_tag, git_ref, repo_ref, multiple, code_lib, container_type, artifactory, sub_pipeline, custom_file, password, do not storage in database) |     | 元素值类型                    |

#### ErrorInfo
##### 插件错误信息

| 参数名称        | 参数类型    | 必须  | 参数说明   |
| ----------- | ------- | --- | ------ |
| atomCode    | string  | √   | 插件编号   |
| containerId | string  |     | 作业ID   |
| errorCode   | integer | √   | 错误码    |
| errorMsg    | string  | √   | 错误信息   |
| errorType   | integer | √   | 错误类型   |
| matrixFlag  | boolean |     | 构建矩阵标识 |
| stageId     | string  |     | 阶段ID   |
| taskId      | string  | √   | 插件ID   |
| taskName    | string  | √   | 插件名称   |

#### PipelineBuildMaterial
##### 原材料

| 参数名称             | 参数类型    | 必须  | 参数说明      |
| ---------------- | ------- | --- | --------- |
| aliasName        | string  |     | 别名        |
| branchName       | string  |     | 分支名称      |
| commitTimes      | integer |     | 提交次数      |
| createTime       | integer |     | 提交时间      |
| mainRepo         | boolean |     | 是否为源材料主仓库 |
| newCommitComment | string  |     | 当前提交备注信息  |
| newCommitId      | string  |     | 当前最新提交id  |
| scmType          | string  |     | 代码库类型     |
| taskId           | string  |     | 插件ID      |
| url              | string  |     | url 地址    |

#### BuildStageStatus
##### 历史构建阶段状态

| 参数名称       | 参数类型                                        | 必须  | 参数说明        |
| ---------- | ------------------------------------------- | --- | ----------- |
| elapsed    | integer                                     |     | 该字段只读容器运行时间 |
| name       | string                                      | √   | 阶段名称        |
| showMsg    | string                                      |     | 该字段只读前端     |
| stageId    | string                                      | √   | 阶段ID        |
| startEpoch | integer                                     |     | 该字段只读阶段启动时间 |
| status     | string                                      |     | 该字段只读阶段状态   |
| tag        | List<string>                                |     | 该字段只读阶段标签   |
| timeCost   | [BuildRecordTimeCost](#BuildRecordTimeCost) |     |             |

#### BuildRecordTimeCost
##### 各项执行耗时（单位毫秒）

| 参数名称        | 参数类型    | 必须  | 参数说明                              |
| ----------- | ------- | --- | --------------------------------- |
| executeCost | integer | √   | 执行耗时                              |
| queueCost   | integer | √   | 只处于排队的耗时（流水线并发、Stage下Job并发和Job互斥） |
| systemCost  | integer | √   | 系统耗时（由总耗时减去其他得出）                  |
| totalCost   | integer | √   | 总耗时（结束时间-开始时间）                    |
| waitCost    | integer | √   | 等待耗时（包括了排队和等待人工审核操作时间）            |

#### WebhookInfo
##### webhook信息

| 参数名称                  | 参数类型   | 必须  | 参数说明                                                  |
| --------------------- | ------ | --- | ----------------------------------------------------- |
| codeType              | string | √   | 代码库类型                                                 |
| issueIid              | string |     |                                                       |
| linkUrl               | string |     |                                                       |
| materialId            | string |     |                                                       |
| materialName          | string |     |                                                       |
| mrId                  | string |     |                                                       |
| mrIid                 | string |     |                                                       |
| mrUrl                 | string |     |                                                       |
| nameWithNamespace     | string | √   | 代码库完整名称                                               |
| noteId                | string |     |                                                       |
| parentBuildId         | string |     |                                                       |
| parentBuildNum        | string |     |                                                       |
| parentPipelineId      | string |     |                                                       |
| parentPipelineName    | string |     |                                                       |
| parentProjectId       | string |     |                                                       |
| refId                 | string | √   | 参考信息(commit_id,mr_id,tag,issue_id,review_id,note_id等) |
| repoAuthUser          | string |     |                                                       |
| reviewId              | string |     |                                                       |
| tagName               | string |     |                                                       |
| webhookAliasName      | string |     | 别名                                                    |
| webhookBranch         | string |     | 分支名（目标分支）                                             |
| webhookCommitId       | string |     | 提交信息id                                                |
| webhookEventType      | string |     | 事件类型                                                  |
| webhookMergeCommitSha | string |     | 合并后commitId                                           |
| webhookMessage        | string |     | 提交信息                                                  |
| webhookRepoUrl        | string |     | 仓库url链接                                               |
| webhookSourceBranch   | string |     | 源分支                                                   |
| webhookType           | string |     | webhook类型                                             |

 
