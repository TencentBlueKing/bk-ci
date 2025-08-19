### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/build_execute_pause
### 资源描述
#### 操作暂停插件
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
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

| 参数名称 | 参数类型                                      | 必须  |
| ---- | ----------------------------------------- | --- |
|      | [BuildTaskPauseInfo](#BuildTaskPauseInfo) |     |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "containerId" : "",
  "continue" : false,
  "element" : {
    "@type" : "",
    "additionalOptions" : {
      "continueWhenFailed" : false,
      "customCondition" : "",
      "customEnv" : [ {
        "key" : "",
        "value" : ""
      } ],
      "customVariables" : [ {
        "key" : "",
        "value" : ""
      } ],
      "elementPostInfo" : {
        "parentElementId" : "",
        "parentElementJobIndex" : 0,
        "parentElementName" : "",
        "postCondition" : "",
        "postEntryParam" : ""
      },
      "enable" : false,
      "enableCustomEnv" : false,
      "manualRetry" : false,
      "manualSkip" : false,
      "otherTask" : "",
      "pauseBeforeExec" : false,
      "retryCount" : 0,
      "retryWhenFailed" : false,
      "runCondition" : "enum",
      "subscriptionPauseUser" : "",
      "timeout" : 0,
      "timeoutVar" : ""
    },
    "asyncStatus" : "",
    "atomCode" : "",
    "atomName" : "",
    "canRetry" : false,
    "canSkip" : false,
    "classType" : "",
    "classifyCode" : "",
    "classifyName" : "",
    "customEnv" : [ {
      "key" : "",
      "value" : ""
    } ],
    "elapsed" : 0,
    "errorCode" : 0,
    "errorMsg" : "",
    "errorType" : "",
    "executeCount" : 0,
    "id" : "",
    "name" : "",
    "originVersion" : "",
    "progressRate" : "number",
    "ref" : "",
    "retryCount" : 0,
    "retryCountAuto" : 0,
    "retryCountManual" : 0,
    "startEpoch" : 0,
    "status" : "",
    "stepId" : "",
    "taskAtom" : "",
    "template" : "",
    "templateModify" : false,
    "timeCost" : {
      "executeCost" : 0,
      "queueCost" : 0,
      "systemCost" : 0,
      "totalCost" : 0,
      "waitCost" : 0
    },
    "variables" : {
      "string" : ""
    },
    "version" : ""
  },
  "stageId" : "",
  "stepId" : "",
  "taskId" : ""
}
```

### default 返回样例

```Json
{
  "data" : false,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### BuildTaskPauseInfo
##### 流水线暂停操作实体类

| 参数名称        | 参数类型                | 必须  | 参数说明                      |
| ----------- | ------------------- | --- | ------------------------- |
| containerId | string              | √   | 当前containerId             |
| continue    | boolean             |     | 是否继续 true:继续构建 false：停止构建 |
| element     | [Element](#Element) |     |                           |
| stageId     | string              | √   | 当前stageId                 |
| stepId      | string              |     | 插件ID                      |
| taskId      | string              |     | 任务ID                      |

#### Element
##### Element 基类

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| @type             | string                                                | √   | 用于指定实现某一多态类, 可选[MatrixStatusElement, CodeGitWebHookTriggerElement, CodeGitlabWebHookTriggerElement, CodeSVNWebHookTriggerElement, CodeGithubWebHookTriggerElement, CodeGitElement, CodeGitlabElement, GithubElement, CodeSvnElement, LinuxScriptElement, WindowsScriptElement, ManualTriggerElement, RemoteTriggerElement, TimerTriggerElement, ManualReviewUserTaskElement, SubPipelineCallElement, MarketBuildAtomElement, MarketBuildLessAtomElement, MarketCheckImageElement, QualityGateInElement, QualityGateOutElement, CodeTGitWebHookTriggerElement, CodeP4WebHookTriggerElement, CodeScmGitWebHookTriggerElement, CodeScmSvnWebHookTriggerElement],具体实现见下方 |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| asyncStatus       | string                                                | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| atomCode          | string                                                | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| classType         | string                                                |     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| id                | string                                                |     | id                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| name              | string                                                | √   | 任务名称                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| progressRate      | number                                                | √   | 任务运行进度                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ref               | string                                                | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| status            | string                                                |     | 状态(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| taskAtom          | string                                                |     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| template          | string                                                | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| variables         | Map<String, string>                                   | √   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| version           | string                                                | √   | 插件版本                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

#### MatrixStatusElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [matrixStatus] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | matrixStatus |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| desc              | string                                                |     | 描述(人工审核插件使用)                                 |
| elapsed           | integer                                               |     | 执行时间                                         |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数                                         |
| id                | string                                                |     | 插件ID                                         |
| interceptTask     | string                                                |     | 拦截原子                                         |
| interceptTaskName | string                                                |     | 拦截原子名称                                       |
| name              | string                                                | √   | 任务名称                                         |
| originAtomCode    | string                                                |     | 原插件的市场标识                                     |
| originClassType   | string                                                | √   | 原插件的类型标识                                     |
| originTaskAtom    | string                                                |     | 原插件的内置标识                                     |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| params            | List<[ManualReviewParam](#ManualReviewParam)>         |     | 参数列表(人工审核插件使用)                               |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| reviewUsers       | List<string>                                          |     | 审核人                                          |
| startEpoch        | integer                                               |     | 启动时间                                         |
| status            | string                                                |     | 执行状态                                         |
| stepId            | string                                                |     | 上下文标识                                        |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### CodeGitWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeGitWebHookTrigger] 时指定为该类实现*
 

| 参数名称                    | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明                                         |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | -------------------------------------------- |
| @type                   | string                                                                                                                                                                                                                                                                                                                            | 必须是 | 多态类实现                                        | codeGitWebHookTrigger |
| additionalOptions       | [ElementAdditionalOptions](#ElementAdditionalOptions)                                                                                                                                                                                                                                                                             | √   |                                              |
| asyncStatus             | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| atomCode                | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| atomName                | string                                                                                                                                                                                                                                                                                                                            | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| block                   | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否为block                                     |
| branchName              | string                                                                                                                                                                                                                                                                                                                            |     | 分支名称                                         |
| canRetry                | boolean                                                                                                                                                                                                                                                                                                                           | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip                 | boolean                                                                                                                                                                                                                                                                                                                           | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType               | string                                                                                                                                                                                                                                                                                                                            |     |                                              |
| classifyCode            | string                                                                                                                                                                                                                                                                                                                            | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName            | string                                                                                                                                                                                                                                                                                                                            | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv               | List<[NameAndValue](#NameAndValue)>                                                                                                                                                                                                                                                                                               |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed                 | integer                                                                                                                                                                                                                                                                                                                           | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableCheck             | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用回写                                       |
| enableThirdFilter       | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用第三方过滤                                    |
| errorCode               | integer                                                                                                                                                                                                                                                                                                                           | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg                | string                                                                                                                                                                                                                                                                                                                            | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType               | string                                                                                                                                                                                                                                                                                                                            | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| eventType               | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型                                         |
| excludeBranchName       | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的分支名                                     |
| excludePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的路径                                      |
| excludeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的源分支名称                                   |
| excludeTagName          | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的tag名称                                   |
| excludeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用于排除的user id                                 |
| executeCount            | integer                                                                                                                                                                                                                                                                                                                           | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| fromBranches            | string                                                                                                                                                                                                                                                                                                                            |     | tag从哪条分支创建                                   |
| id                      | string                                                                                                                                                                                                                                                                                                                            |     | id                                           |
| includeCrState          | List<string>                                                                                                                                                                                                                                                                                                                      |     | code review 状态                               |
| includeCrTypes          | List<string>                                                                                                                                                                                                                                                                                                                      |     | code review 类型                               |
| includeIssueAction      | List<string>                                                                                                                                                                                                                                                                                                                      |     | issue事件action                                |
| includeMrAction         | List<string>                                                                                                                                                                                                                                                                                                                      |     | mr事件action                                   |
| includeNoteComment      | string                                                                                                                                                                                                                                                                                                                            |     | code note comment                            |
| includeNoteTypes        | List<string>                                                                                                                                                                                                                                                                                                                      |     | code note 类型                                 |
| includePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的路径                                      |
| includePushAction       | List<string>                                                                                                                                                                                                                                                                                                                      |     | push事件action                                 |
| includeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的源分支名称                                   |
| includeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用户白名单                                        |
| name                    | string                                                                                                                                                                                                                                                                                                                            | √   | 任务名称                                         |
| originVersion           | string                                                                                                                                                                                                                                                                                                                            | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| pathFilterType          | ENUM(NamePrefixFilter, RegexBasedFilter)                                                                                                                                                                                                                                                                                          |     | 路径过滤类型                                       |
| progressRate            | number                                                                                                                                                                                                                                                                                                                            | √   | 任务运行进度                                       |
| ref                     | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| repositoryHashId        | string                                                                                                                                                                                                                                                                                                                            |     | 仓库ID                                         |
| repositoryName          | string                                                                                                                                                                                                                                                                                                                            |     | 新版的git代码库名                                   |
| repositoryType          | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的git原子的类型                                  |
| retryCount              | integer                                                                                                                                                                                                                                                                                                                           | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto          | integer                                                                                                                                                                                                                                                                                                                           | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual        | integer                                                                                                                                                                                                                                                                                                                           | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| skipWip                 | boolean                                                                                                                                                                                                                                                                                                                           |     | 跳过WIP                                        |
| startEpoch              | integer                                                                                                                                                                                                                                                                                                                           | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status                  | string                                                                                                                                                                                                                                                                                                                            |     | 状态                                           |
| stepId                  | string                                                                                                                                                                                                                                                                                                                            | √   | 用户自定义ID，用于上下文键值设置                            |
| tagName                 | string                                                                                                                                                                                                                                                                                                                            |     | tag名称                                        |
| taskAtom                | string                                                                                                                                                                                                                                                                                                                            |     |                                              |
| template                | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| templateModify          | boolean                                                                                                                                                                                                                                                                                                                           | √   | 模板对比的时候是不是有变更(temporary field)               |
| thirdSecretToken        | string                                                                                                                                                                                                                                                                                                                            |     | 第三方应用鉴权token                                 |
| thirdUrl                | string                                                                                                                                                                                                                                                                                                                            |     | 第三方应用地址                                      |
| timeCost                | [BuildRecordTimeCost](#BuildRecordTimeCost)                                                                                                                                                                                                                                                                                       | √   |                                              |
| variables               | Map<String, string>                                                                                                                                                                                                                                                                                                               | √   |                                              |
| version                 | string                                                                                                                                                                                                                                                                                                                            | √   | 插件版本                                         |
| webhookQueue            | boolean                                                                                                                                                                                                                                                                                                                           |     | webhook队列                                    |

#### CodeGitlabWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeGitlabWebHookTrigger] 时指定为该类实现*
 

| 参数名称                    | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明                                         |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | -------------------------------------------- |
| @type                   | string                                                                                                                                                                                                                                                                                                                            | 必须是 | 多态类实现                                        | codeGitlabWebHookTrigger |
| additionalOptions       | [ElementAdditionalOptions](#ElementAdditionalOptions)                                                                                                                                                                                                                                                                             | √   |                                              |
| asyncStatus             | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| atomCode                | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| atomName                | string                                                                                                                                                                                                                                                                                                                            | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| block                   | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否为block                                     |
| branchName              | string                                                                                                                                                                                                                                                                                                                            |     | 分支名称                                         |
| canRetry                | boolean                                                                                                                                                                                                                                                                                                                           | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip                 | boolean                                                                                                                                                                                                                                                                                                                           | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType               | string                                                                                                                                                                                                                                                                                                                            |     |                                              |
| classifyCode            | string                                                                                                                                                                                                                                                                                                                            | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName            | string                                                                                                                                                                                                                                                                                                                            | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv               | List<[NameAndValue](#NameAndValue)>                                                                                                                                                                                                                                                                                               |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed                 | integer                                                                                                                                                                                                                                                                                                                           | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode               | integer                                                                                                                                                                                                                                                                                                                           | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg                | string                                                                                                                                                                                                                                                                                                                            | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType               | string                                                                                                                                                                                                                                                                                                                            | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| eventType               | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型                                         |
| excludeBranchName       | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的分支名                                     |
| excludeCommitMsg        | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的提交信息                                    |
| excludePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的路径                                      |
| excludeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的源分支名称                                   |
| excludeTagName          | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的tag名称                                   |
| excludeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用于排除的user id                                 |
| executeCount            | integer                                                                                                                                                                                                                                                                                                                           | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                      | string                                                                                                                                                                                                                                                                                                                            |     | id                                           |
| includeCommitMsg        | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的提交信息                                    |
| includeMrAction         | List<string>                                                                                                                                                                                                                                                                                                                      |     | mr事件action                                   |
| includePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的路径                                      |
| includePushAction       | List<string>                                                                                                                                                                                                                                                                                                                      |     | push事件action                                 |
| includeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的源分支名称                                   |
| includeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用于包含的user id                                 |
| name                    | string                                                                                                                                                                                                                                                                                                                            | √   | 任务名称                                         |
| originVersion           | string                                                                                                                                                                                                                                                                                                                            | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| pathFilterType          | ENUM(NamePrefixFilter, RegexBasedFilter)                                                                                                                                                                                                                                                                                          |     | 路径过滤类型                                       |
| progressRate            | number                                                                                                                                                                                                                                                                                                                            | √   | 任务运行进度                                       |
| ref                     | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| repositoryHashId        | string                                                                                                                                                                                                                                                                                                                            |     | 仓库ID                                         |
| repositoryName          | string                                                                                                                                                                                                                                                                                                                            |     | 新版的gitlab代码库名                                |
| repositoryType          | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的gitlab原子的类型                               |
| retryCount              | integer                                                                                                                                                                                                                                                                                                                           | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto          | integer                                                                                                                                                                                                                                                                                                                           | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual        | integer                                                                                                                                                                                                                                                                                                                           | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch              | integer                                                                                                                                                                                                                                                                                                                           | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status                  | string                                                                                                                                                                                                                                                                                                                            |     | 状态                                           |
| stepId                  | string                                                                                                                                                                                                                                                                                                                            | √   | 用户自定义ID，用于上下文键值设置                            |
| tagName                 | string                                                                                                                                                                                                                                                                                                                            |     | tag名称                                        |
| taskAtom                | string                                                                                                                                                                                                                                                                                                                            |     |                                              |
| template                | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| templateModify          | boolean                                                                                                                                                                                                                                                                                                                           | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost                | [BuildRecordTimeCost](#BuildRecordTimeCost)                                                                                                                                                                                                                                                                                       | √   |                                              |
| variables               | Map<String, string>                                                                                                                                                                                                                                                                                                               | √   |                                              |
| version                 | string                                                                                                                                                                                                                                                                                                                            | √   | 插件版本                                         |

#### CodeSVNWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeSVNWebHookTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | codeSVNWebHookTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| excludePaths      | string                                                |     | 排除的路径                                        |
| excludeUsers      | List<string>                                          |     | 用户黑名单                                        |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| includeUsers      | List<string>                                          |     | 用户白名单                                        |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| pathFilterType    | ENUM(NamePrefixFilter, RegexBasedFilter)              |     | 路径过滤类型                                       |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| relativePath      | string                                                |     | 相对路径                                         |
| repositoryHashId  | string                                                |     | 仓库ID                                         |
| repositoryName    | string                                                |     | 新版的svn代码库名                                   |
| repositoryType    | ENUM(ID, NAME, SELF, NONE)                            |     | 新版的svn原子的类型                                  |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### CodeGithubWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeGithubWebHookTrigger] 时指定为该类实现*
 

| 参数名称                    | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明                                         |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | -------------------------------------------- |
| @type                   | string                                                                                                                                                                                                                                                                                                                            | 必须是 | 多态类实现                                        | codeGithubWebHookTrigger |
| additionalOptions       | [ElementAdditionalOptions](#ElementAdditionalOptions)                                                                                                                                                                                                                                                                             | √   |                                              |
| asyncStatus             | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| atomCode                | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| atomName                | string                                                                                                                                                                                                                                                                                                                            | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| branchName              | string                                                                                                                                                                                                                                                                                                                            |     | 分支名称                                         |
| canRetry                | boolean                                                                                                                                                                                                                                                                                                                           | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip                 | boolean                                                                                                                                                                                                                                                                                                                           | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType               | string                                                                                                                                                                                                                                                                                                                            |     |                                              |
| classifyCode            | string                                                                                                                                                                                                                                                                                                                            | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName            | string                                                                                                                                                                                                                                                                                                                            | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv               | List<[NameAndValue](#NameAndValue)>                                                                                                                                                                                                                                                                                               |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed                 | integer                                                                                                                                                                                                                                                                                                                           | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableCheck             | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用回写                                       |
| enableThirdFilter       | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用第三方过滤                                    |
| errorCode               | integer                                                                                                                                                                                                                                                                                                                           | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg                | string                                                                                                                                                                                                                                                                                                                            | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType               | string                                                                                                                                                                                                                                                                                                                            | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| eventType               | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型                                         |
| excludeBranchName       | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的分支名称                                    |
| excludePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的路径                                      |
| excludeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的源分支名称                                   |
| excludeTagName          | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的tag名称                                   |
| excludeUsers            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的user id                                 |
| executeCount            | integer                                                                                                                                                                                                                                                                                                                           | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| fromBranches            | string                                                                                                                                                                                                                                                                                                                            |     | tag从哪条分支创建                                   |
| id                      | string                                                                                                                                                                                                                                                                                                                            |     | id                                           |
| includeCrState          | List<string>                                                                                                                                                                                                                                                                                                                      |     | code review 状态                               |
| includeCrTypes          | List<string>                                                                                                                                                                                                                                                                                                                      |     | code review 类型                               |
| includeIssueAction      | List<string>                                                                                                                                                                                                                                                                                                                      |     | issue事件action                                |
| includeMrAction         | List<string>                                                                                                                                                                                                                                                                                                                      |     | pull request事件action                         |
| includeNoteComment      | string                                                                                                                                                                                                                                                                                                                            |     | code note comment                            |
| includeNoteTypes        | List<string>                                                                                                                                                                                                                                                                                                                      |     | code note 类型                                 |
| includePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的路径                                      |
| includePushAction       | List<string>                                                                                                                                                                                                                                                                                                                      |     | push事件action                                 |
| includeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的源分支名称                                   |
| includeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用户白名单                                        |
| name                    | string                                                                                                                                                                                                                                                                                                                            | √   | 任务名称                                         |
| originVersion           | string                                                                                                                                                                                                                                                                                                                            | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| pathFilterType          | ENUM(NamePrefixFilter, RegexBasedFilter)                                                                                                                                                                                                                                                                                          |     | 路径过滤类型                                       |
| progressRate            | number                                                                                                                                                                                                                                                                                                                            | √   | 任务运行进度                                       |
| ref                     | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| repositoryHashId        | string                                                                                                                                                                                                                                                                                                                            |     | 仓库ID                                         |
| repositoryName          | string                                                                                                                                                                                                                                                                                                                            |     | 新版的github代码库名                                |
| repositoryType          | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的github原子的类型                               |
| retryCount              | integer                                                                                                                                                                                                                                                                                                                           | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto          | integer                                                                                                                                                                                                                                                                                                                           | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual        | integer                                                                                                                                                                                                                                                                                                                           | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch              | integer                                                                                                                                                                                                                                                                                                                           | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status                  | string                                                                                                                                                                                                                                                                                                                            |     | 状态                                           |
| stepId                  | string                                                                                                                                                                                                                                                                                                                            | √   | 用户自定义ID，用于上下文键值设置                            |
| tagName                 | string                                                                                                                                                                                                                                                                                                                            |     | tag名称                                        |
| taskAtom                | string                                                                                                                                                                                                                                                                                                                            |     |                                              |
| template                | string                                                                                                                                                                                                                                                                                                                            | √   |                                              |
| templateModify          | boolean                                                                                                                                                                                                                                                                                                                           | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost                | [BuildRecordTimeCost](#BuildRecordTimeCost)                                                                                                                                                                                                                                                                                       | √   |                                              |
| variables               | Map<String, string>                                                                                                                                                                                                                                                                                                               | √   |                                              |
| version                 | string                                                                                                                                                                                                                                                                                                                            | √   | 插件版本                                         |
| webhookQueue            | boolean                                                                                                                                                                                                                                                                                                                           |     | webhook队列                                    |

#### CodeGitElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [CODE_GIT] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | CODE_GIT |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| branchName        | string                                                |     | 分支名称                                         |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableSubmodule   | boolean                                               |     | 启动Submodule                                  |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| gitPullMode       | [GitPullMode](#GitPullMode)                           |     |                                              |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| path              | string                                                |     | 代码存放路径                                       |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| repositoryHashId  | string                                                |     | 代码库哈希ID                                      |
| repositoryName    | string                                                |     | 新版的git代码库名                                   |
| repositoryType    | ENUM(ID, NAME)                                        |     | 新版的git插件的类型                                  |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| revision          | string                                                |     | revision 用于强制指定commitId                      |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| strategy          | ENUM(FRESH_CHECKOUT, INCREMENT_UPDATE, REVERT_UPDATE) |     | checkout 策略                                  |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### CodeGitlabElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [CODE_GITLAB] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | CODE_GITLAB |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| branchName        | string                                                |     | 分支名称                                         |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableSubmodule   | boolean                                               |     | 启动Submodule                                  |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| gitPullMode       | [GitPullMode](#GitPullMode)                           |     |                                              |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| path              | string                                                |     | 代码存放路径                                       |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| repositoryHashId  | string                                                |     | 代码库哈希ID                                      |
| repositoryName    | string                                                |     | 新版的gitlab代码库名                                |
| repositoryType    | ENUM(ID, NAME)                                        |     | 新版的gitlab原子的类型                               |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| revision          | string                                                |     | revision 用于强制指定commitId                      |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| strategy          | ENUM(FRESH_CHECKOUT, INCREMENT_UPDATE, REVERT_UPDATE) |     | checkout 策略                                  |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### GithubElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [GITHUB] 时指定为该类实现*
 

| 参数名称                     | 参数类型                                                  | 必须  | 参数说明                                         |
| ------------------------ | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type                    | string                                                | 必须是 | 多态类实现                                        | GITHUB |
| additionalOptions        | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus              | string                                                | √   |                                              |
| atomCode                 | string                                                | √   |                                              |
| atomName                 | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry                 | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip                  | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType                | string                                                |     |                                              |
| classifyCode             | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName             | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv                | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed                  | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableSubmodule          | boolean                                               |     | 启动Submodule                                  |
| enableVirtualMergeBranch | boolean                                               |     | 支持虚拟合并分支                                     |
| errorCode                | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg                 | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType                | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount             | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| gitPullMode              | [GitPullMode](#GitPullMode)                           |     |                                              |
| id                       | string                                                |     | id                                           |
| name                     | string                                                | √   | 任务名称                                         |
| originVersion            | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| path                     | string                                                |     | 代码存放路径                                       |
| progressRate             | number                                                | √   | 任务运行进度                                       |
| ref                      | string                                                | √   |                                              |
| repositoryHashId         | string                                                |     | 代码库哈希ID                                      |
| repositoryName           | string                                                |     | 新版的github代码库名                                |
| repositoryType           | ENUM(ID, NAME)                                        |     | 新版的github原子的类型                               |
| retryCount               | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto           | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual         | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| revision                 | string                                                |     | revision 用于强制指定commitId                      |
| startEpoch               | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status                   | string                                                |     | 状态                                           |
| stepId                   | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| strategy                 | ENUM(FRESH_CHECKOUT, INCREMENT_UPDATE, REVERT_UPDATE) |     | checkout 策略                                  |
| taskAtom                 | string                                                |     |                                              |
| template                 | string                                                | √   |                                              |
| templateModify           | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost                 | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables                | Map<String, string>                                   | √   |                                              |
| version                  | string                                                | √   | 插件版本                                         |

#### CodeSvnElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [CODE_SVN] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | CODE_SVN |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableSubmodule   | boolean                                               |     | 启动Submodule                                  |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| path              | string                                                |     | 代码存放路径                                       |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| repositoryHashId  | string                                                |     | 代码库哈希ID                                      |
| repositoryName    | string                                                |     | 新版的svn代码库名                                   |
| repositoryType    | ENUM(ID, NAME)                                        |     | 新版的svn原子的类型                                  |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| revision          | string                                                |     | revision 用于强制指定commitId                      |
| specifyRevision   | boolean                                               |     | 指定版本号                                        |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| strategy          | ENUM(FRESH_CHECKOUT, INCREMENT_UPDATE, REVERT_UPDATE) |     | checkout 策略                                  |
| svnDepth          | ENUM(empty, files, immediates, infinity)              |     | 拉取仓库深度                                       |
| svnPath           | string                                                |     | SVN相对路径                                      |
| svnVersion        | ENUM(V_1_6, V_1_7, V_1_8)                             |     | SVN的版本                                       |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### LinuxScriptElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [linuxScript] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | linuxScript |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| archiveFile       | string                                                |     | 脚本执行失败时归档的文件                                 |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| continueNoneZero  | boolean                                               |     | 某次执行为非0时（失败）是否继续执行脚本                         |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| enableArchiveFile | boolean                                               |     | 启用脚本执行失败时归档的文件                               |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorFAQUrl       | string                                                |     | FAQ url链接                                    |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| script            | string                                                | √   | 脚本内容                                         |
| scriptType        | ENUM(PYTHON2, PYTHON3, SHELL, BAT, POWER_SHELL)       | √   | 脚本类型                                         |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                |     | 用户自定义ID                                      |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### WindowsScriptElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [windowsScript] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | windowsScript |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| charsetType       | ENUM(DEFAULT, UTF_8, GBK)                             |     | 字符集类型                                        |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorFAQUrl       | string                                                |     | FAQ url链接                                    |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| script            | string                                                | √   | 脚本内容                                         |
| scriptType        | ENUM(PYTHON2, PYTHON3, SHELL, BAT, POWER_SHELL)       | √   | 脚本类型                                         |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                |     | 用户自定义ID                                      |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### ManualTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [manualTrigger] 时指定为该类实现*
 

| 参数名称                | 参数类型                                                  | 必须  | 参数说明                                         |
| ------------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type               | string                                                | 必须是 | 多态类实现                                        | manualTrigger |
| additionalOptions   | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus         | string                                                | √   |                                              |
| atomCode            | string                                                | √   |                                              |
| atomName            | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canElementSkip      | boolean                                               |     | 是否可跳过插件                                      |
| canRetry            | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip             | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType           | string                                                |     |                                              |
| classifyCode        | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName        | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv           | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed             | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode           | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg            | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType           | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount        | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                  | string                                                |     | id                                           |
| name                | string                                                | √   | 任务名称                                         |
| originVersion       | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate        | number                                                | √   | 任务运行进度                                       |
| ref                 | string                                                | √   |                                              |
| retryCount          | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto      | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual    | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch          | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status              | string                                                |     | 状态                                           |
| stepId              | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom            | string                                                |     |                                              |
| template            | string                                                | √   |                                              |
| templateModify      | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost            | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| useLatestParameters | boolean                                               |     | 是否使用最近一次的参数进行构建                              |
| variables           | Map<String, string>                                   | √   |                                              |
| version             | string                                                | √   | 插件版本                                         |

#### RemoteTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [remoteTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | remoteTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canElementSkip    | boolean                                               |     | 是否可跳过插件                                      |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| remoteToken       | string                                                | √   | 远程触发token                                    |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### TimerTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [timerTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                                     |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                                    | timerTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                                          |
| advanceExpression | List<string>                                          |     | 高级定时表达式                                                  |
| asyncStatus       | string                                                | √   |                                                          |
| atomCode          | string                                                | √   |                                                          |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）             |
| branches          | List<string>                                          |     | 指定代码库分支                                                  |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| classType         | string                                                |     |                                                          |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                    |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                    |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                                     |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| expression        | string                                                |     | 定时表达式                                                    |
| id                | string                                                |     | id                                                       |
| name              | string                                                | √   | 任务名称                                                     |
| newExpression     | List<string>                                          |     | 改进后的表达式                                                  |
| noScm             | boolean                                               |     | 源代码未更新则不触发构建                                             |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                      |
| progressRate      | number                                                | √   | 任务运行进度                                                   |
| ref               | string                                                | √   |                                                          |
| repoHashId        | string                                                |     | 代码库HashId                                                |
| repoName          | string                                                |     | 指定代码库别名                                                  |
| repositoryType    | ENUM(ID, NAME, SELF, NONE)                            |     | 代码库类型                                                    |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                       |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                      |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                      |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）                        |
| startParams       | string                                                |     | 定时启动参数,格式: [{key:'id',value:1},{key:'name',value:'xxx'}] |
| status            | string                                                |     | 状态                                                       |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                                        |
| taskAtom          | string                                                |     |                                                          |
| template          | string                                                | √   |                                                          |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)                           |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                                          |
| variables         | Map<String, string>                                   | √   |                                                          |
| version           | string                                                | √   | 插件版本                                                     |

#### ManualReviewUserTaskElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [manualReviewUserTask] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | manualReviewUserTask |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| desc              | string                                                |     | 描述                                           |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| markdownContent   | boolean                                               |     | 是否以markdown格式发送审核说明                          |
| name              | string                                                | √   | 任务名称                                         |
| namespace         | string                                                |     | 输出变量名空间                                      |
| notifyGroup       | List<string>                                          |     | 企业微信群id                                      |
| notifyTitle       | string                                                |     | 发送通知的标题                                      |
| notifyType        | List<string>                                          |     | 发送的通知类型                                      |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| params            | List<[ManualReviewParam](#ManualReviewParam)>         | √   | 参数列表                                         |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| reminderTime      | integer                                               |     | 审核提醒时间（小时），支持每隔x小时提醒一次                       |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| reviewUsers       | List<string>                                          | √   | 审核人                                          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| suggest           | string                                                |     | 审核意见                                         |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### SubPipelineCallElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [subPipelineCall] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | subPipelineCall |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| asynchronous      | boolean                                               | √   | 是否异步                                         |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| parameters        | Map<String, string>                                   |     | 启动参数                                         |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| subPipelineId     | string                                                | √   | 子流水线ID                                       |
| subPipelineName   | string                                                |     | 新版的子流水线名                                     |
| subPipelineType   | ENUM(ID, NAME)                                        |     | 新版的子流水线原子的类型                                 |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### MarketBuildAtomElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [marketBuild] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | marketBuild |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| autoAtomCode      | string                                                |     | 插件的唯一标识                                      |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| data              | Map<String, Any>                                      | √   | 插件参数数据                                       |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id将由后台生成                                     |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                |     | 用户自定义ID                                      |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### MarketBuildLessAtomElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [marketBuildLess] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | marketBuildLess |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| autoAtomCode      | string                                                |     | 插件的唯一标识                                      |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| data              | Map<String, Any>                                      | √   | 插件参数数据                                       |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id将由后台生成                                     |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                |     | 用户自定义ID                                      |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### MarketCheckImageElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [marketCheckImage] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | marketCheckImage |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| registryPwd       | string                                                |     | 密码                                           |
| registryUser      | string                                                |     | 用户名                                          |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### QualityGateInElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [qualityGateInTask] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | qualityGateInTask |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| interceptTask     | string                                                |     | 拦截原子                                         |
| interceptTaskName | string                                                |     | 拦截原子名称                                       |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| reviewUsers       | List<string>                                          |     | 审核人                                          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### QualityGateOutElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [qualityGateOutTask] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | qualityGateOutTask |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| interceptTask     | string                                                |     | 拦截原子                                         |
| interceptTaskName | string                                                |     | 拦截原子名称                                       |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| reviewUsers       | List<string>                                          |     | 审核人                                          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### CodeTGitWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeTGitWebHookTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                      | 必须  | 参数说明                                         |
| ----------------- | --------------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                    | 必须是 | 多态类实现                                        | codeTGitWebHookTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions)     | √   |                                              |
| asyncStatus       | string                                                    | √   |                                              |
| atomCode          | string                                                    | √   |                                              |
| atomName          | string                                                    | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                                   | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                                   | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                    |     |                                              |
| classifyCode      | string                                                    | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                    | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                       |     | 用户自定义环境变量（插件运行时写入环境）                         |
| data              | [CodeTGitWebHookTriggerData](#CodeTGitWebHookTriggerData) | √   |                                              |
| elapsed           | integer                                                   | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                                   | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                    | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                    | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                                   | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                    |     | id                                           |
| name              | string                                                    | √   | 任务名称                                         |
| originVersion     | string                                                    | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                    | √   | 任务运行进度                                       |
| ref               | string                                                    | √   |                                              |
| retryCount        | integer                                                   | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                                   | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                                   | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                                   | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                    |     | 状态                                           |
| stepId            | string                                                    | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                    |     |                                              |
| template          | string                                                    | √   |                                              |
| templateModify    | boolean                                                   | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)               | √   |                                              |
| variables         | Map<String, string>                                       | √   |                                              |
| version           | string                                                    | √   | 插件版本                                         |

#### CodeP4WebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeP4WebHookTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                  | 必须  | 参数说明                                         |
| ----------------- | ----------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                | 必须是 | 多态类实现                                        | codeP4WebHookTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions) | √   |                                              |
| asyncStatus       | string                                                | √   |                                              |
| atomCode          | string                                                | √   |                                              |
| atomName          | string                                                | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                               | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                               | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                |     |                                              |
| classifyCode      | string                                                | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                   |     | 用户自定义环境变量（插件运行时写入环境）                         |
| data              | [CodeP4WebHookTriggerData](#CodeP4WebHookTriggerData) | √   |                                              |
| elapsed           | integer                                               | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                               | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                               | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                |     | id                                           |
| name              | string                                                | √   | 任务名称                                         |
| originVersion     | string                                                | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                | √   | 任务运行进度                                       |
| ref               | string                                                | √   |                                              |
| retryCount        | integer                                               | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                               | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                               | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                               | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                |     | 状态                                           |
| stepId            | string                                                | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                |     |                                              |
| template          | string                                                | √   |                                              |
| templateModify    | boolean                                               | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)           | √   |                                              |
| variables         | Map<String, string>                                   | √   |                                              |
| version           | string                                                | √   | 插件版本                                         |

#### CodeScmGitWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeScmGitWebHookTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                          | 必须  | 参数说明                                         |
| ----------------- | ------------------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                        | 必须是 | 多态类实现                                        | codeScmGitWebHookTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions)         | √   |                                              |
| asyncStatus       | string                                                        | √   |                                              |
| atomCode          | string                                                        | √   |                                              |
| atomName          | string                                                        | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                                       | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                                       | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                        |     |                                              |
| classifyCode      | string                                                        | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                        | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                           |     | 用户自定义环境变量（插件运行时写入环境）                         |
| data              | [CodeScmGitWebHookTriggerData](#CodeScmGitWebHookTriggerData) | √   |                                              |
| elapsed           | integer                                                       | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                                       | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                        | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                        | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                                       | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                        |     | id                                           |
| name              | string                                                        | √   | 任务名称                                         |
| originVersion     | string                                                        | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                        | √   | 任务运行进度                                       |
| ref               | string                                                        | √   |                                              |
| retryCount        | integer                                                       | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                                       | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                                       | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                                       | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                        |     | 状态                                           |
| stepId            | string                                                        | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                        |     |                                              |
| template          | string                                                        | √   |                                              |
| templateModify    | boolean                                                       | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)                   | √   |                                              |
| variables         | Map<String, string>                                           | √   |                                              |
| version           | string                                                        | √   | 插件版本                                         |

#### CodeScmSvnWebHookTriggerElement
 *多态基类 <Element> 的实现处, 其中当字段 @type = [codeScmSvnWebHookTrigger] 时指定为该类实现*
 

| 参数名称              | 参数类型                                                          | 必须  | 参数说明                                         |
| ----------------- | ------------------------------------------------------------- | --- | -------------------------------------------- |
| @type             | string                                                        | 必须是 | 多态类实现                                        | codeScmSvnWebHookTrigger |
| additionalOptions | [ElementAdditionalOptions](#ElementAdditionalOptions)         | √   |                                              |
| asyncStatus       | string                                                        | √   |                                              |
| atomCode          | string                                                        | √   |                                              |
| atomName          | string                                                        | √   | 插件名称,构建结束后的快照名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值） |
| canRetry          | boolean                                                       | √   | 是否重试(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| canSkip           | boolean                                                       | √   | 是否跳过(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| classType         | string                                                        |     |                                              |
| classifyCode      | string                                                        | √   | 所属插件分类代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| classifyName      | string                                                        | √   | 所属插件分类名称(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）        |
| customEnv         | List<[NameAndValue](#NameAndValue)>                           |     | 用户自定义环境变量（插件运行时写入环境）                         |
| data              | [CodeScmSvnWebHookTriggerData](#CodeScmSvnWebHookTriggerData) | √   |                                              |
| elapsed           | integer                                                       | √   | 执行时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorCode         | integer                                                       | √   | 错误代码(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorMsg          | string                                                        | √   | 错误信息(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| errorType         | string                                                        | √   | 错误类型(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| executeCount      | integer                                                       | √   | 执行次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| id                | string                                                        |     | id                                           |
| name              | string                                                        | √   | 任务名称                                         |
| originVersion     | string                                                        | √   | 插件原始版本(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| progressRate      | number                                                        | √   | 任务运行进度                                       |
| ref               | string                                                        | √   |                                              |
| retryCount        | integer                                                       | √   | 总重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）           |
| retryCountAuto    | integer                                                       | √   | 自动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| retryCountManual  | integer                                                       | √   | 手动重试次数(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）          |
| startEpoch        | integer                                                       | √   | 启动时间(仅在运行构建时有用的中间参数，不要在编排保存阶段设置值）            |
| status            | string                                                        |     | 状态                                           |
| stepId            | string                                                        | √   | 用户自定义ID，用于上下文键值设置                            |
| taskAtom          | string                                                        |     |                                              |
| template          | string                                                        | √   |                                              |
| templateModify    | boolean                                                       | √   | 模板对比的时候是不是有变更(temporary field)               |
| timeCost          | [BuildRecordTimeCost](#BuildRecordTimeCost)                   | √   |                                              |
| variables         | Map<String, string>                                           | √   |                                              |
| version           | string                                                        | √   | 插件版本                                         |

#### ElementAdditionalOptions
##### 插件级别流程控制模型

| 参数名称                  | 参数类型                                                                                                                                                                                                                                                         | 必须  | 参数说明                                    |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --- | --------------------------------------- |
| continueWhenFailed    | boolean                                                                                                                                                                                                                                                      | √   | 是否失败时继续                                 |
| customCondition       | string                                                                                                                                                                                                                                                       |     | 自定义条件                                   |
| customEnv             | List<[NameAndValue](#NameAndValue)>                                                                                                                                                                                                                          |     | 用户自定义环境变量（插件运行时写入环境）                    |
| customVariables       | List<[NameAndValue](#NameAndValue)>                                                                                                                                                                                                                          |     | 自定义变量                                   |
| elementPostInfo       | [ElementPostInfo](#ElementPostInfo)                                                                                                                                                                                                                          |     |                                         |
| enable                | boolean                                                                                                                                                                                                                                                      | √   | 是否启用                                    |
| enableCustomEnv       | boolean                                                                                                                                                                                                                                                      |     | 是否设置自定义环境变量                             |
| manualRetry           | boolean                                                                                                                                                                                                                                                      | √   | 是否允许手动重试                                |
| manualSkip            | boolean                                                                                                                                                                                                                                                      |     | 是否出现跳过按钮（手动继续）                          |
| otherTask             | string                                                                                                                                                                                                                                                       |     |                                         |
| pauseBeforeExec       | boolean                                                                                                                                                                                                                                                      |     | 是否配置前置暂停                                |
| retryCount            | integer                                                                                                                                                                                                                                                      | √   | 重试计数                                    |
| retryWhenFailed       | boolean                                                                                                                                                                                                                                                      | √   | 是否失败时重试                                 |
| runCondition          | ENUM(PRE_TASK_SUCCESS, PRE_TASK_FAILED_BUT_CANCEL, PRE_TASK_FAILED_EVEN_CANCEL, PRE_TASK_FAILED_ONLY, OTHER_TASK_RUNNING, CUSTOM_VARIABLE_MATCH, CUSTOM_VARIABLE_MATCH_NOT_RUN, CUSTOM_CONDITION_MATCH, PARENT_TASK_CANCELED_OR_TIMEOUT, PARENT_TASK_FINISH) |     | 执行条件                                    |
| subscriptionPauseUser | string                                                                                                                                                                                                                                                       |     | 订阅暂停通知用户                                |
| timeout               | integer                                                                                                                                                                                                                                                      |     | 超时分钟                                    |
| timeoutVar            | string                                                                                                                                                                                                                                                       |     | 新的执行的超时时间，支持变量(分钟Minutes)，出错则取timeout的值 |

#### NameAndValue
##### 用户自定义环境变量（Agent启动时写入环境）

| 参数名称  | 参数类型   | 必须  | 参数说明 |
| ----- | ------ | --- | ---- |
| key   | string |     |      |
| value | string |     |      |

#### ElementPostInfo
##### 元素post信息

| 参数名称                  | 参数类型    | 必须  | 参数说明        |
| --------------------- | ------- | --- | ----------- |
| parentElementId       | string  | √   | 父元素ID       |
| parentElementJobIndex | integer | √   | 父元素在job中的位置 |
| parentElementName     | string  | √   | 父元素名称       |
| postCondition         | string  | √   | 执行条件        |
| postEntryParam        | string  | √   | 入口参数        |

#### BuildRecordTimeCost
##### 各项执行耗时（单位毫秒）

| 参数名称        | 参数类型    | 必须  | 参数说明                              |
| ----------- | ------- | --- | --------------------------------- |
| executeCost | integer | √   | 执行耗时                              |
| queueCost   | integer | √   | 只处于排队的耗时（流水线并发、Stage下Job并发和Job互斥） |
| systemCost  | integer | √   | 系统耗时（由总耗时减去其他得出）                  |
| totalCost   | integer | √   | 总耗时（结束时间-开始时间）                    |
| waitCost    | integer | √   | 等待耗时（包括了排队和等待人工审核操作时间）            |

#### ManualReviewParam
##### 人工审核-自定义参数

| 参数名称           | 参数类型                                                      | 必须  | 参数说明         |
| -------------- | --------------------------------------------------------- | --- | ------------ |
| chineseName    | string                                                    |     | 中文名称         |
| desc           | string                                                    |     | 参数描述         |
| key            | string                                                    | √   | 参数名          |
| options        | List<[ManualReviewParamPair](#ManualReviewParamPair)>     |     | 下拉框列表        |
| required       | boolean                                                   | √   | 是否必填         |
| value          | string                                                    |     | 参数内容(Any 类型) |
| valueType      | ENUM(string, textarea, boolean, enum, checkbox, multiple) | √   | 参数类型         |
| variableOption | string                                                    |     | 变量形式的options |

#### ManualReviewParamPair
##### 人工审核-自定义参数-下拉框列表剑

| 参数名称  | 参数类型   | 必须  | 参数说明 |
| ----- | ------ | --- | ---- |
| key   | string | √   | 参数名  |
| value | string | √   | 参数内容 |

#### GitPullMode
##### 指定拉取方式

| 参数名称  | 参数类型                         | 必须  | 参数说明 |
| ----- | ---------------------------- | --- | ---- |
| type  | ENUM(BRANCH, TAG, COMMIT_ID) |     |      |
| value | string                       |     |      |

#### CodeTGitWebHookTriggerData
##### 数据

| 参数名称  | 参数类型                                                        | 必须  | 参数说明 |
| ----- | ----------------------------------------------------------- | --- | ---- |
| input | [CodeTGitWebHookTriggerInput](#CodeTGitWebHookTriggerInput) |     |      |

#### CodeTGitWebHookTriggerInput
##### TGit事件触发数据

| 参数名称                    | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明              |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ----------------- |
| block                   | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否为block          |
| branchName              | string                                                                                                                                                                                                                                                                                                                            |     | 分支名称              |
| enableCheck             | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用回写            |
| enableThirdFilter       | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用第三方过滤         |
| eventType               | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型              |
| excludeBranchName       | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的分支名          |
| excludePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的路径           |
| excludeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的源分支名称        |
| excludeTagName          | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的tag名称        |
| excludeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用于排除的user id      |
| fromBranches            | string                                                                                                                                                                                                                                                                                                                            |     | tag从哪条分支创建        |
| includeCrState          | List<string>                                                                                                                                                                                                                                                                                                                      |     | code review 状态    |
| includeCrTypes          | List<string>                                                                                                                                                                                                                                                                                                                      |     | code review 类型    |
| includeIssueAction      | List<string>                                                                                                                                                                                                                                                                                                                      |     | issue事件action     |
| includeMrAction         | List<string>                                                                                                                                                                                                                                                                                                                      |     | mr事件action        |
| includeNoteComment      | string                                                                                                                                                                                                                                                                                                                            |     | code note comment |
| includeNoteTypes        | List<string>                                                                                                                                                                                                                                                                                                                      |     | code note 类型      |
| includePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的路径           |
| includePushAction       | List<string>                                                                                                                                                                                                                                                                                                                      |     | push事件action      |
| includeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的源分支名称        |
| includeUsers            | List<string>                                                                                                                                                                                                                                                                                                                      |     | 用户白名单             |
| pathFilterType          | ENUM(NamePrefixFilter, RegexBasedFilter)                                                                                                                                                                                                                                                                                          |     | 路径过滤类型            |
| repositoryHashId        | string                                                                                                                                                                                                                                                                                                                            |     | 仓库ID              |
| repositoryName          | string                                                                                                                                                                                                                                                                                                                            |     | 新版的git代码库名        |
| repositoryType          | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的git原子的类型       |
| skipWip                 | boolean                                                                                                                                                                                                                                                                                                                           |     | 跳过WIP             |
| tagName                 | string                                                                                                                                                                                                                                                                                                                            |     | tag名称             |
| webhookQueue            | boolean                                                                                                                                                                                                                                                                                                                           |     | webhook队列         |

#### CodeP4WebHookTriggerData
##### 数据

| 参数名称  | 参数类型                                                    | 必须  | 参数说明 |
| ----- | ------------------------------------------------------- | --- | ---- |
| input | [CodeP4WebHookTriggerInput](#CodeP4WebHookTriggerInput) |     |      |

#### CodeP4WebHookTriggerInput
##### 

| 参数名称             | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明        |
| ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ----------- |
| eventType        | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型        |
| excludePaths     | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的路径     |
| includePaths     | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的路径     |
| repositoryHashId | string                                                                                                                                                                                                                                                                                                                            | √   | 仓库ID        |
| repositoryName   | string                                                                                                                                                                                                                                                                                                                            |     | 新版的git代码库名  |
| repositoryType   | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的git原子的类型 |

#### CodeScmGitWebHookTriggerData
##### 数据

| 参数名称  | 参数类型                                                            | 必须  | 参数说明 |
| ----- | --------------------------------------------------------------- | --- | ---- |
| input | [CodeScmGitWebHookTriggerInput](#CodeScmGitWebHookTriggerInput) |     |      |

#### CodeScmGitWebHookTriggerInput
##### ScmGit事件触发数据

| 参数名称                    | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明         |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------ |
| actions                 | List<string>                                                                                                                                                                                                                                                                                                                      |     | 事件动作         |
| block                   | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否为block     |
| branchName              | string                                                                                                                                                                                                                                                                                                                            |     | 分支名称         |
| enableCheck             | boolean                                                                                                                                                                                                                                                                                                                           |     | 是否启用回写       |
| eventType               | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型         |
| excludeBranchName       | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的分支名     |
| excludePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的路径      |
| excludeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的源分支名称   |
| excludeTagName          | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的tag名称   |
| excludeUsers            | string                                                                                                                                                                                                                                                                                                                            |     | 用于排除的user id |
| includePaths            | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的路径      |
| includeSourceBranchName | string                                                                                                                                                                                                                                                                                                                            |     | 用于包含的源分支名称   |
| includeUsers            | string                                                                                                                                                                                                                                                                                                                            |     | 用户白名单        |
| pathFilterType          | ENUM(NamePrefixFilter, RegexBasedFilter)                                                                                                                                                                                                                                                                                          | √   | 路径过滤类型       |
| repositoryHashId        | string                                                                                                                                                                                                                                                                                                                            | √   | 仓库ID         |
| repositoryName          | string                                                                                                                                                                                                                                                                                                                            |     | 新版的git代码库名   |
| repositoryType          | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的git原子的类型  |
| scmCode                 | string                                                                                                                                                                                                                                                                                                                            | √   | 代码库标识        |
| tagName                 | string                                                                                                                                                                                                                                                                                                                            |     | tag名称        |

#### CodeScmSvnWebHookTriggerData
##### 数据

| 参数名称  | 参数类型                                                            | 必须  | 参数说明 |
| ----- | --------------------------------------------------------------- | --- | ---- |
| input | [CodeScmSvnWebHookTriggerInput](#CodeScmSvnWebHookTriggerInput) |     |      |

#### CodeScmSvnWebHookTriggerInput
##### ScmSvn事件触发数据

| 参数名称             | 参数类型                                                                                                                                                                                                                                                                                                                              | 必须  | 参数说明        |
| ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ----------- |
| eventType        | ENUM(PUSH, TAG_PUSH, MERGE_REQUEST, MERGE_REQUEST_ACCEPT, ISSUES, NOTE, REVIEW, CREATE, PULL_REQUEST, POST_COMMIT, LOCK_COMMIT, PRE_COMMIT, CHANGE_COMMIT, PUSH_SUBMIT, CHANGE_CONTENT, CHANGE_SUBMIT, PUSH_CONTENT, PUSH_COMMIT, FIX_ADD, FIX_DELETE, FORM_COMMIT, SHELVE_COMMIT, SHELVE_DELETE, SHELVE_SUBMIT, PARENT_PIPELINE) |     | 事件类型        |
| excludePaths     | string                                                                                                                                                                                                                                                                                                                            |     | 排除的路径       |
| excludeUsers     | string                                                                                                                                                                                                                                                                                                                            |     | 用户黑名单       |
| includeUsers     | string                                                                                                                                                                                                                                                                                                                            |     | 用户白名单       |
| pathFilterType   | ENUM(NamePrefixFilter, RegexBasedFilter)                                                                                                                                                                                                                                                                                          | √   | 路径过滤类型      |
| relativePath     | string                                                                                                                                                                                                                                                                                                                            | √   | 相对路径        |
| repositoryHashId | string                                                                                                                                                                                                                                                                                                                            | √   | 仓库ID        |
| repositoryName   | string                                                                                                                                                                                                                                                                                                                            |     | 新版的svn代码库名  |
| repositoryType   | ENUM(ID, NAME, SELF, NONE)                                                                                                                                                                                                                                                                                                        |     | 新版的svn原子的类型 |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
