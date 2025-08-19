### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/build_detail
### 资源描述
#### 构建详情
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称         | 参数类型    | 必须  | 参数说明     |
| ------------ | ------- | --- | -------- |
| archiveFlag  | boolean |     | 是否查询归档数据 |
| buildId      | String  | √   | 构建ID     |
| executeCount | integer |     | 执行次数     |
| pipelineId   | String  | √   | 流水线ID    |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                    | 说明               |
| ------- | --------------------------------------- | ---------------- |
| default | [ResultModelRecord](#ResultModelRecord) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?archiveFlag={archiveFlag}&buildId={buildId}&executeCount={executeCount}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "artifactQuality" : {
      "string" : [ {
        "color" : "",
        "count" : 0,
        "labelKey" : "",
        "value" : ""
      } ]
    },
    "buildMsg" : "",
    "buildNum" : 0,
    "cancelUserId" : "",
    "curVersion" : 0,
    "curVersionName" : "",
    "currentTimestamp" : 0,
    "debug" : false,
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
    "executeCount" : 0,
    "executeTime" : 0,
    "id" : "",
    "lastModifyUser" : "",
    "latestBuildNum" : 0,
    "latestVersion" : 0,
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
    "model" : {
      "desc" : "",
      "events" : {
        "string" : {
          "callbackEvent" : "enum",
          "callbackName" : "",
          "callbackUrl" : "",
          "region" : "enum",
          "secretToken" : ""
        }
      },
      "instanceFromTemplate" : false,
      "labels" : [ "" ],
      "latestVersion" : 0,
      "name" : "",
      "pipelineCreator" : "",
      "ref" : "",
      "resources" : {
        "pools" : [ {
          "from" : "",
          "name" : ""
        } ],
        "repositories" : [ {
          "credentials" : {
            "personal-access-token" : "",
            "personalAccessToken" : ""
          },
          "name" : "",
          "ref" : "",
          "repository" : ""
        } ]
      },
      "srcTemplateId" : "",
      "stages" : [ {
        "canRetry" : false,
        "checkIn" : {
          "checkTimes" : 0,
          "manualTrigger" : false,
          "markdownContent" : false,
          "notifyGroup" : [ "" ],
          "notifyType" : [ "" ],
          "reviewDesc" : "",
          "reviewGroups" : [ {
            "groups" : [ "" ],
            "id" : "",
            "name" : "",
            "operator" : "",
            "params" : [ {
              "chineseName" : "",
              "desc" : "",
              "key" : "",
              "options" : [ {
                "key" : "",
                "value" : ""
              } ],
              "required" : false,
              "value" : "",
              "valueType" : "enum",
              "variableOption" : ""
            } ],
            "reviewTime" : 0,
            "reviewers" : [ "" ],
            "status" : "",
            "suggest" : ""
          } ],
          "reviewParams" : [ {
            "chineseName" : "",
            "desc" : "",
            "key" : "",
            "options" : [ {
              "key" : "",
              "value" : ""
            } ],
            "required" : false,
            "value" : "",
            "valueType" : "enum",
            "variableOption" : ""
          } ],
          "ruleIds" : [ "" ],
          "status" : "",
          "timeout" : 0
        },
        "checkOut" : {
          "checkTimes" : 0,
          "manualTrigger" : false,
          "markdownContent" : false,
          "notifyGroup" : [ "" ],
          "notifyType" : [ "" ],
          "reviewDesc" : "",
          "reviewGroups" : [ {
            "groups" : [ "" ],
            "id" : "",
            "name" : "",
            "operator" : "",
            "params" : [ {
              "chineseName" : "",
              "desc" : "",
              "key" : "",
              "options" : [ {
                "key" : "",
                "value" : ""
              } ],
              "required" : false,
              "value" : "",
              "valueType" : "enum",
              "variableOption" : ""
            } ],
            "reviewTime" : 0,
            "reviewers" : [ "" ],
            "status" : "",
            "suggest" : ""
          } ],
          "reviewParams" : [ {
            "chineseName" : "",
            "desc" : "",
            "key" : "",
            "options" : [ {
              "key" : "",
              "value" : ""
            } ],
            "required" : false,
            "value" : "",
            "valueType" : "enum",
            "variableOption" : ""
          } ],
          "ruleIds" : [ "" ],
          "status" : "",
          "timeout" : 0
        },
        "containers" : [ {
          "@type" : "",
          "canRetry" : false,
          "classType" : "",
          "containPostTaskFlag" : false,
          "containerEnable" : false,
          "containerHashId" : "",
          "containerId" : "",
          "elementElapsed" : 0,
          "elements" : [ {
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
          } ],
          "executeCount" : 0,
          "id" : "",
          "jobId" : "",
          "matrixGroupFlag" : false,
          "name" : "",
          "ref" : "",
          "startEpoch" : 0,
          "startVMStatus" : "",
          "startVMTaskSeq" : 0,
          "status" : "",
          "systemElapsed" : 0,
          "template" : "",
          "timeCost" : {
            "executeCost" : 0,
            "queueCost" : 0,
            "systemCost" : 0,
            "totalCost" : 0,
            "waitCost" : 0
          },
          "variables" : {
            "string" : ""
          }
        } ],
        "customBuildEnv" : {
          "string" : ""
        },
        "elapsed" : 0,
        "executeCount" : 0,
        "fastKill" : false,
        "finally" : false,
        "id" : "",
        "name" : "",
        "ref" : "",
        "stageControlOption" : {
          "customCondition" : "",
          "customVariables" : [ {
            "key" : "",
            "value" : ""
          } ],
          "enable" : false,
          "manualTrigger" : false,
          "reviewDesc" : "",
          "reviewParams" : [ {
            "chineseName" : "",
            "desc" : "",
            "key" : "",
            "options" : [ {
              "key" : "",
              "value" : ""
            } ],
            "required" : false,
            "value" : "",
            "valueType" : "enum",
            "variableOption" : ""
          } ],
          "runCondition" : "enum",
          "timeout" : 0,
          "triggerUsers" : [ "" ],
          "triggered" : false
        },
        "stageIdForUser" : "",
        "startEpoch" : 0,
        "status" : "",
        "tag" : [ "" ],
        "template" : "",
        "timeCost" : {
          "executeCost" : 0,
          "queueCost" : 0,
          "systemCost" : 0,
          "totalCost" : 0,
          "waitCost" : 0
        },
        "variables" : {
          "string" : ""
        }
      } ],
      "staticViews" : [ "" ],
      "template" : "",
      "templateId" : "",
      "timeCost" : {
        "executeCost" : 0,
        "queueCost" : 0,
        "systemCost" : 0,
        "totalCost" : 0,
        "waitCost" : 0
      },
      "tips" : "",
      "triggerContainer" : {
        "buildNo" : {
          "buildNo" : 0,
          "buildNoType" : "enum",
          "currentBuildNo" : 0,
          "required" : false
        },
        "canRetry" : false,
        "classType" : "",
        "containPostTaskFlag" : false,
        "containerEnable" : false,
        "containerHashId" : "",
        "containerId" : "",
        "elementElapsed" : 0,
        "elements" : [ {
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
        } ],
        "executeCount" : 0,
        "id" : "",
        "jobId" : "",
        "matrixGroupFlag" : false,
        "name" : "",
        "params" : [ {
          "cascadeProps" : {
            "children" : "#/components/schemas/BuildCascadeProps",
            "id" : "",
            "options" : [ {
              "key" : "",
              "value" : ""
            } ],
            "replaceKey" : "",
            "searchUrl" : ""
          },
          "category" : "",
          "constant" : false,
          "containerType" : {
            "buildType" : "enum",
            "os" : "enum"
          },
          "defaultValue" : "Any 任意类型，参照实际请求或返回",
          "desc" : "",
          "displayCondition" : {
            "string" : ""
          },
          "enableVersionControl" : false,
          "glob" : "",
          "id" : "",
          "label" : "",
          "latestRandomStringInPath" : "",
          "name" : "",
          "options" : [ {
            "key" : "",
            "value" : ""
          } ],
          "payload" : "Any 任意类型，参照实际请求或返回",
          "placeholder" : "",
          "properties" : {
            "string" : ""
          },
          "propertyType" : "",
          "randomStringInPath" : "",
          "readOnly" : false,
          "relativePath" : "",
          "replaceKey" : "",
          "repoHashId" : "",
          "required" : false,
          "scmType" : "enum",
          "searchUrl" : "",
          "type" : "enum",
          "value" : "Any 任意类型，参照实际请求或返回",
          "valueNotEmpty" : false
        } ],
        "ref" : "",
        "startEpoch" : 0,
        "startVMStatus" : "",
        "startVMTaskSeq" : 0,
        "status" : "",
        "systemElapsed" : 0,
        "template" : "",
        "templateParams" : [ {
          "cascadeProps" : {
            "children" : "#/components/schemas/BuildCascadeProps",
            "id" : "",
            "options" : [ {
              "key" : "",
              "value" : ""
            } ],
            "replaceKey" : "",
            "searchUrl" : ""
          },
          "category" : "",
          "constant" : false,
          "containerType" : {
            "buildType" : "enum",
            "os" : "enum"
          },
          "defaultValue" : "Any 任意类型，参照实际请求或返回",
          "desc" : "",
          "displayCondition" : {
            "string" : ""
          },
          "enableVersionControl" : false,
          "glob" : "",
          "id" : "",
          "label" : "",
          "latestRandomStringInPath" : "",
          "name" : "",
          "options" : [ {
            "key" : "",
            "value" : ""
          } ],
          "payload" : "Any 任意类型，参照实际请求或返回",
          "placeholder" : "",
          "properties" : {
            "string" : ""
          },
          "propertyType" : "",
          "randomStringInPath" : "",
          "readOnly" : false,
          "relativePath" : "",
          "replaceKey" : "",
          "repoHashId" : "",
          "required" : false,
          "scmType" : "enum",
          "searchUrl" : "",
          "type" : "enum",
          "value" : "Any 任意类型，参照实际请求或返回",
          "valueNotEmpty" : false
        } ],
        "timeCost" : {
          "executeCost" : 0,
          "queueCost" : 0,
          "systemCost" : 0,
          "totalCost" : 0,
          "waitCost" : 0
        },
        "variables" : {
          "string" : ""
        }
      },
      "variables" : {
        "string" : ""
      }
    },
    "pipelineId" : "",
    "pipelineName" : "",
    "queueTime" : 0,
    "queueTimeCost" : 0,
    "recordList" : [ {
      "startUser" : "",
      "timeCost" : {
        "executeCost" : 0,
        "queueCost" : 0,
        "systemCost" : 0,
        "totalCost" : 0,
        "waitCost" : 0
      }
    } ],
    "remark" : "",
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
    "startUserList" : [ "" ],
    "status" : "",
    "templateInfo" : {
      "desc" : "",
      "instanceType" : "enum",
      "templateId" : "",
      "templateName" : "",
      "version" : 0,
      "versionName" : ""
    },
    "trigger" : "",
    "triggerReviewers" : [ "" ],
    "triggerUser" : "",
    "userId" : "",
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
#### ResultModelRecord
##### 数据返回包装模型

| 参数名称    | 参数类型                        | 必须  | 参数说明 |
| ------- | --------------------------- | --- | ---- |
| data    | [ModelRecord](#ModelRecord) |     |      |
| message | string                      |     | 错误信息 |
| status  | integer                     | √   | 状态码  |

#### ModelRecord
##### 构建详情-构建信息

| 参数名称             | 参数类型                                                                                     | 必须  | 参数说明                    |
| ---------------- | ---------------------------------------------------------------------------------------- | --- | ----------------------- |
| artifactQuality  | Map<String, List<[ArtifactQualityMetadataAnalytics](#ArtifactQualityMetadataAnalytics)>> |     | 制品质量分析                  |
| buildMsg         | string                                                                                   |     | 构建信息                    |
| buildNum         | integer                                                                                  | √   | 构建号                     |
| cancelUserId     | string                                                                                   |     | 取消构建的用户                 |
| curVersion       | integer                                                                                  | √   | 本次执行的编排版本号              |
| curVersionName   | string                                                                                   |     | 本次执行的编排版本名              |
| currentTimestamp | integer                                                                                  | √   | 服务器当前时间戳                |
| debug            | boolean                                                                                  |     | 是否为调试构建                 |
| endTime          | integer                                                                                  |     | 执行结束时间                  |
| errorInfoList    | List<[ErrorInfo](#ErrorInfo)>                                                            |     | 流水线任务执行错误               |
| executeCount     | integer                                                                                  | √   | 当前查询的执行次数               |
| executeTime      | integer                                                                                  | √   | 执行耗时（排除系统耗时）流水线执行结束时才赋值 |
| id               | string                                                                                   | √   | 构建ID                    |
| lastModifyUser   | string                                                                                   |     | 最近修改人                   |
| latestBuildNum   | integer                                                                                  | √   | 最新一次的构建buildNo          |
| latestVersion    | integer                                                                                  | √   | 流水线当前最新版本号              |
| material         | List<[PipelineBuildMaterial](#PipelineBuildMaterial)>                                    |     | 原材料                     |
| model            | [Model](#Model)                                                                          | √   |                         |
| pipelineId       | string                                                                                   | √   | 流水线ID                   |
| pipelineName     | string                                                                                   | √   | 流水线名称                   |
| queueTime        | integer                                                                                  | √   | 触发时间（进队列时间）             |
| queueTimeCost    | integer                                                                                  |     | 排队耗时（进队列到开始执行）          |
| recordList       | List<[BuildRecordInfo](#BuildRecordInfo)>                                                | √   | 历史重试人列表（有序）             |
| remark           | string                                                                                   |     | 备注                      |
| stageStatus      | List<[BuildStageStatus](#BuildStageStatus)>                                              |     | 已执行stage的状态             |
| startTime        | integer                                                                                  |     | 执行开始时间                  |
| startUserList    | List<string>                                                                             | √   | 历史重试执行人列表（有序）           |
| status           | string                                                                                   | √   | Build status            |
| templateInfo     | [TemplateInfo](#TemplateInfo)                                                            |     |                         |
| trigger          | string                                                                                   | √   | 触发条件                    |
| triggerReviewers | List<string>                                                                             |     | 触发审核人列表                 |
| triggerUser      | string                                                                                   |     | 触发用户                    |
| userId           | string                                                                                   | √   | 启动用户                    |
| webhookInfo      | [WebhookInfo](#WebhookInfo)                                                              |     |                         |

#### ArtifactQualityMetadataAnalytics
##### 制品质量元数据分析

| 参数名称     | 参数类型    | 必须  | 参数说明 |
| -------- | ------- | --- | ---- |
| color    | string  |     |      |
| count    | integer | √   |      |
| labelKey | string  | √   |      |
| value    | string  | √   |      |

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

#### Model
##### 流水线模型-创建信息

| 参数名称                 | 参数类型                                                         | 必须  | 参数说明                          |
| -------------------- | ------------------------------------------------------------ | --- | ----------------------------- |
| desc                 | string                                                       |     | 描述                            |
| events               | Map<String, [PipelineCallbackEvent](#PipelineCallbackEvent)> |     | 流水线事件回调                       |
| instanceFromTemplate | boolean                                                      |     | 是否从模板中实例化出来的                  |
| labels               | List<string>                                                 | √   | 标签                            |
| latestVersion        | integer                                                      | √   | 提交时流水线最新版本号                   |
| name                 | string                                                       | √   | 名称                            |
| pipelineCreator      | string                                                       |     | 创建人                           |
| ref                  | string                                                       |     | 模板版本                          |
| resources            | [Resources](#Resources)                                      |     |                               |
| srcTemplateId        | string                                                       |     | 当前模板对应的被复制的模板或安装的研发商店的模板对应的ID |
| stages               | List<[Stage](#Stage)>                                        | √   | 阶段集合                          |
| staticViews          | List<string>                                                 | √   | 静态流水线组                        |
| template             | string                                                       |     | 模板地址                          |
| templateId           | string                                                       |     | 当前模板的ID                       |
| timeCost             | [BuildRecordTimeCost](#BuildRecordTimeCost)                  |     |                               |
| tips                 | string                                                       |     | 提示                            |
| triggerContainer     | [TriggerContainer](#TriggerContainer)                        |     |                               |
| variables            | Map<String, string>                                          |     | 模板入参                          |

#### PipelineCallbackEvent
##### 流水线事件回调

| 参数名称          | 参数类型                                                                                                                                                                                                                                                                                                       | 必须  | 参数说明 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ---- |
| callbackEvent | ENUM(DELETE_PIPELINE, CREATE_PIPELINE, UPDATE_PIPELINE, STREAM_ENABLED, RESTORE_PIPELINE, BUILD_START, BUILD_END, BUILD_STAGE_START, BUILD_STAGE_END, BUILD_JOB_START, BUILD_JOB_END, BUILD_TASK_START, BUILD_TASK_END, BUILD_TASK_PAUSE, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_ENABLE, PROJECT_DISABLE) |     |      |
| callbackName  | string                                                                                                                                                                                                                                                                                                     |     |      |
| callbackUrl   | string                                                                                                                                                                                                                                                                                                     |     |      |
| region        | ENUM(DEVNET, OSS, IDC)                                                                                                                                                                                                                                                                                     |     |      |
| secretToken   | string                                                                                                                                                                                                                                                                                                     |     |      |

#### Resources
##### 模板资源

| 参数名称         | 参数类型                                    | 必须  | 参数说明 |
| ------------ | --------------------------------------- | --- | ---- |
| pools        | List<[ResourcesPools](#ResourcesPools)> |     |      |
| repositories | List<[Repositories](#Repositories)>     |     |      |

#### ResourcesPools
##### 

| 参数名称 | 参数类型   | 必须  | 参数说明 |
| ---- | ------ | --- | ---- |
| from | string |     |      |
| name | string |     |      |

#### Repositories
##### 

| 参数名称        | 参数类型                              | 必须  | 参数说明 |
| ----------- | --------------------------------- | --- | ---- |
| credentials | [ResCredentials](#ResCredentials) |     |      |
| name        | string                            |     |      |
| ref         | string                            |     |      |
| repository  | string                            |     |      |

#### ResCredentials
##### 

| 参数名称                  | 参数类型   | 必须  | 参数说明                  |
| --------------------- | ------ | --- | --------------------- |
| personal-access-token | string |     |                       |
| personalAccessToken   | string |     | personal-access-token |

#### Stage
##### 流水线模型-阶段

| 参数名称               | 参数类型                                        | 必须  | 参数说明                                                 |
| ------------------ | ------------------------------------------- | --- | ---------------------------------------------------- |
| canRetry           | boolean                                     |     | 当前Stage是否能重试                                         |
| checkIn            | [StagePauseCheck](#StagePauseCheck)         |     |                                                      |
| checkOut           | [StagePauseCheck](#StagePauseCheck)         |     |                                                      |
| containers         | List<[Container](#Container)>               | √   | 容器集合                                                 |
| customBuildEnv     | Map<String, string>                         |     | 用户自定义环境变量                                            |
| elapsed            | integer                                     |     | 该字段只读容器运行时间                                          |
| executeCount       | integer                                     |     | 该字段只读步骤运行次数                                          |
| fastKill           | boolean                                     |     | 是否启用容器失败快速终止阶段                                       |
| finally            | boolean                                     | √   | 标识是否为FinallyStage，每个Model只能包含一个FinallyStage，并且处于最后位置 |
| id                 | string                                      |     | 阶段ID (系统标识，用户不可编辑)                                   |
| name               | string                                      |     | 阶段名称                                                 |
| ref                | string                                      |     |                                                      |
| stageControlOption | [StageControlOption](#StageControlOption)   |     |                                                      |
| stageIdForUser     | string                                      |     | 阶段ID (用户可编辑)                                         |
| startEpoch         | integer                                     |     | 该字段只读阶段启动时间                                          |
| status             | string                                      |     | 该字段只读阶段状态                                            |
| tag                | List<string>                                |     | 该字段只读阶段标签                                            |
| template           | string                                      |     |                                                      |
| timeCost           | [BuildRecordTimeCost](#BuildRecordTimeCost) |     |                                                      |
| variables          | Map<String, string>                         |     |                                                      |

#### StagePauseCheck
##### stage准入准出配置模型

| 参数名称            | 参数类型                                          | 必须  | 参数说明                |
| --------------- | --------------------------------------------- | --- | ------------------- |
| checkTimes      | integer                                       |     | 记录本次构建质量红线规则的检查次数   |
| manualTrigger   | boolean                                       |     | 是否人工触发              |
| markdownContent | boolean                                       |     | 是否以markdown格式发送审核说明 |
| notifyGroup     | List<string>                                  |     | 企业微信群id             |
| notifyType      | List<string>                                  |     | 发送的通知类型             |
| reviewDesc      | string                                        |     | 审核说明                |
| reviewGroups    | List<[StageReviewGroup](#StageReviewGroup)>   |     | 审核流配置               |
| reviewParams    | List<[ManualReviewParam](#ManualReviewParam)> |     | 审核变量                |
| ruleIds         | List<string>                                  |     | 质量红线规则ID集合          |
| status          | string                                        |     | 状态                  |
| timeout         | integer                                       |     | 等待审核的超时时间，默认24小时兜底  |

#### StageReviewGroup
##### Stage审核组信息

| 参数名称       | 参数类型                                          | 必须  | 参数说明        |
| ---------- | --------------------------------------------- | --- | ----------- |
| groups     | List<string>                                  | √   | 审核用户组       |
| id         | string                                        |     | 审核组ID(后台生成) |
| name       | string                                        | √   | 审核组名称       |
| operator   | string                                        |     | 审核操作人       |
| params     | List<[ManualReviewParam](#ManualReviewParam)> |     | 审核传入变量      |
| reviewTime | integer                                       |     | 审核操作时间      |
| reviewers  | List<string>                                  | √   | 审核人员        |
| status     | string                                        |     | 审核结果（枚举）    |
| suggest    | string                                        |     | 审核建议        |

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

#### Container
##### 流水线模型-多态基类

| 参数名称                | 参数类型                                        | 必须  | 参数说明                                                                         |
| ------------------- | ------------------------------------------- | --- | ---------------------------------------------------------------------------- |
| @type               | string                                      | √   | 用于指定实现某一多态类, 可选[TriggerContainer, NormalContainer, VMBuildContainer],具体实现见下方 |
| canRetry            | boolean                                     | √   |                                                                              |
| classType           | string                                      |     |                                                                              |
| containPostTaskFlag | boolean                                     | √   |                                                                              |
| containerEnable     | boolean                                     |     |                                                                              |
| containerHashId     | string                                      | √   |                                                                              |
| containerId         | string                                      | √   |                                                                              |
| elementElapsed      | integer                                     | √   |                                                                              |
| elements            | List<[Element](#Element)>                   | √   |                                                                              |
| executeCount        | integer                                     | √   |                                                                              |
| id                  | string                                      | √   |                                                                              |
| jobId               | string                                      | √   |                                                                              |
| matrixGroupFlag     | boolean                                     | √   |                                                                              |
| name                | string                                      | √   |                                                                              |
| ref                 | string                                      |     |                                                                              |
| startEpoch          | integer                                     | √   |                                                                              |
| startVMStatus       | string                                      | √   |                                                                              |
| startVMTaskSeq      | integer                                     | √   |                                                                              |
| status              | string                                      | √   |                                                                              |
| systemElapsed       | integer                                     | √   |                                                                              |
| template            | string                                      |     |                                                                              |
| timeCost            | [BuildRecordTimeCost](#BuildRecordTimeCost) |     |                                                                              |
| variables           | Map<String, string>                         |     |                                                                              |

#### TriggerContainer
 *多态基类 <Container> 的实现处, 其中当字段 @type = [trigger] 时指定为该类实现*
 

| 参数名称                | 参数类型                                          | 必须  | 参数说明                                          |
| ------------------- | --------------------------------------------- | --- | --------------------------------------------- |
| @type               | string                                        | 必须是 | 多态类实现                                         | trigger |
| buildNo             | [BuildNo](#BuildNo)                           |     |                                               |
| canRetry            | boolean                                       |     | 该字段只读是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储 |
| classType           | string                                        |     |                                               |
| containPostTaskFlag | boolean                                       |     | 该字段只读是否包含post任务标识                             |
| containerEnable     | boolean                                       |     |                                               |
| containerHashId     | string                                        |     | 该字段只读容器唯一ID                                   |
| containerId         | string                                        |     | 该字段只读构建容器顺序ID（同id值）                           |
| elementElapsed      | integer                                       |     | 该字段只读插件执行耗时                                   |
| elements            | List<[Element](#Element)>                     | √   | 任务集合                                          |
| executeCount        | integer                                       |     | 该字段只读容器运行次数                                   |
| id                  | string                                        |     | 该字段只读构建容器序号id                                 |
| jobId               | string                                        |     | 用户自定义ID                                       |
| matrixGroupFlag     | boolean                                       |     | 该字段只读是否为构建矩阵                                  |
| name                | string                                        | √   | 容器名称                                          |
| params              | List<[BuildFormProperty](#BuildFormProperty)> | √   | 参数化构建                                         |
| ref                 | string                                        |     |                                               |
| startEpoch          | integer                                       |     | 系统运行时间                                        |
| startVMStatus       | string                                        |     | 该字段只读构建环境启动状态                                 |
| startVMTaskSeq      | integer                                       |     | 该字段只读开机任务序号                                   |
| status              | string                                        |     | 该字段只读状态                                       |
| systemElapsed       | integer                                       |     | 该字段只读系统耗时（开机时间）                               |
| template            | string                                        |     |                                               |
| templateParams      | List<[BuildFormProperty](#BuildFormProperty)> |     | 模板参数构建                                        |
| timeCost            | [BuildRecordTimeCost](#BuildRecordTimeCost)   |     |                                               |
| variables           | Map<String, string>                           |     |                                               |

#### NormalContainer
 *多态基类 <Container> 的实现处, 其中当字段 @type = [normal] 时指定为该类实现*
 

| 参数名称                | 参数类型                                        | 必须  | 参数说明                                          |
| ------------------- | ------------------------------------------- | --- | --------------------------------------------- |
| @type               | string                                      | 必须是 | 多态类实现                                         | normal |
| canRetry            | boolean                                     |     | 该字段只读是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储 |
| classType           | string                                      |     |                                               |
| conditions          | List<[NameAndValue](#NameAndValue)>         |     | 触发条件                                          |
| containPostTaskFlag | boolean                                     |     | 该字段只读是否包含post任务标识                             |
| containerEnable     | boolean                                     |     |                                               |
| containerHashId     | string                                      |     | 该字段只读容器唯一ID                                   |
| containerId         | string                                      |     | 该字段只读构建容器顺序ID（同id值）                           |
| elementElapsed      | integer                                     |     | 该字段只读插件执行耗时                                   |
| elements            | List<[Element](#Element)>                   | √   | 任务集合                                          |
| enableSkip          | boolean                                     |     | 允许可跳过                                         |
| executeCount        | integer                                     |     | 该字段只读容器运行次数                                   |
| groupContainers     | List<[NormalContainer](#NormalContainer)>   |     | 分裂后的容器集合（分裂后的父容器特有字段）                         |
| id                  | string                                      |     | 该字段只读构建容器序号id                                 |
| jobControlOption    | [JobControlOption](#JobControlOption)       |     |                                               |
| jobId               | string                                      |     | 用户自定义ID                                       |
| matrixContext       | Map<String, string>                         |     | 当前矩阵子容器的上下文组合（分裂后的子容器特有字段）                    |
| matrixControlOption | [MatrixControlOption](#MatrixControlOption) |     |                                               |
| matrixGroupFlag     | boolean                                     |     | 该字段只读是否为构建矩阵                                  |
| matrixGroupId       | string                                      |     | 所在构建矩阵组的containerHashId（分裂后的子容器特有字段）          |
| maxQueueMinutes     | integer                                     | √   | 无构建环境-等待运行环境启动的排队最长时间(分钟)                     |
| maxRunningMinutes   | integer                                     | √   | 无构建环境-运行最长时间(分钟)                              |
| mutexGroup          | [MutexGroup](#MutexGroup)                   |     |                                               |
| name                | string                                      | √   | 容器名称                                          |
| ref                 | string                                      |     |                                               |
| startEpoch          | integer                                     |     | 该字段只读系统运行时间                                   |
| startVMStatus       | string                                      |     | 该字段只读构建环境启动状态                                 |
| startVMTaskSeq      | integer                                     |     | 该字段只读开机任务序号                                   |
| status              | string                                      |     | 该字段只读容器状态                                     |
| systemElapsed       | integer                                     |     | 该字段只读系统耗时（开机时间）                               |
| template            | string                                      |     |                                               |
| timeCost            | [BuildRecordTimeCost](#BuildRecordTimeCost) |     |                                               |
| variables           | Map<String, string>                         |     |                                               |

#### VMBuildContainer
 *多态基类 <Container> 的实现处, 其中当字段 @type = [vmBuild] 时指定为该类实现*
 

| 参数名称                 | 参数类型                                        | 必须  | 参数说明                                          |
| -------------------- | ------------------------------------------- | --- | --------------------------------------------- |
| @type                | string                                      | 必须是 | 多态类实现                                         | vmBuild |
| baseOS               | ENUM(MACOS, LINUX, WINDOWS, ALL)            | √   | VM基础操作系统                                      |
| buildEnv             | Map<String, string>                         |     | 构建机环境变量（容器启动时写入环境）                            |
| canRetry             | boolean                                     |     | 该字段只读是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储 |
| classType            | string                                      |     |                                               |
| containPostTaskFlag  | boolean                                     |     | 该字段只读是否包含post任务标识                             |
| containerEnable      | boolean                                     |     |                                               |
| containerHashId      | string                                      |     | 该字段只读容器唯一ID                                   |
| containerId          | string                                      |     | 该字段只读构建容器顺序ID（同id值）                           |
| customBuildEnv       | Map<String, string>                         |     | 用户自定义环境变量（Agent启动时写入环境）                       |
| customEnv            | List<[NameAndValue](#NameAndValue)>         |     | 用户自定义环境变量（Agent启动时写入环境）                       |
| dispatchType         | [DispatchType](#DispatchType)               |     |                                               |
| dockerBuildVersion   | string                                      |     | Docker构建机                                     |
| elementElapsed       | integer                                     |     | 该字段只读插件执行耗时                                   |
| elements             | List<[Element](#Element)>                   | √   | 任务集合                                          |
| enableExternal       | boolean                                     |     | 该字段只读是否访问外网                                   |
| executeCount         | integer                                     |     | 该字段只读容器运行次数                                   |
| groupContainers      | List<[VMBuildContainer](#VMBuildContainer)> |     | 分裂后的容器集合（分裂后的父容器特有字段）                         |
| id                   | string                                      |     | 该字段只读构建容器序号id                                 |
| jobControlOption     | [JobControlOption](#JobControlOption)       |     |                                               |
| jobId                | string                                      |     | 用户自定义ID                                       |
| matrixContext        | Map<String, string>                         |     | 当前矩阵子容器的上下文组合（分裂后的子容器特有字段）                    |
| matrixControlOption  | [MatrixControlOption](#MatrixControlOption) |     |                                               |
| matrixGroupFlag      | boolean                                     |     | 该字段只读是否为构建矩阵                                  |
| matrixGroupId        | string                                      |     | 所在构建矩阵组的containerHashId（分裂后的子容器特有字段）          |
| maxQueueMinutes      | integer                                     |     | 排队最长时间(分钟)                                    |
| maxRunningMinutes    | integer                                     | √   | 运行最长时间(分钟)                                    |
| mutexGroup           | [MutexGroup](#MutexGroup)                   |     |                                               |
| name                 | string                                      | √   | 容器名称                                          |
| nfsSwitch            | boolean                                     | √   | 该字段只读nfs挂载开关                                  |
| ref                  | string                                      |     |                                               |
| showBuildResource    | boolean                                     |     | 是否显示构建资源信息                                    |
| startEpoch           | integer                                     |     | 该字段只读系统运行时间                                   |
| startVMStatus        | string                                      |     | 该字段只读构建环境启动状态                                 |
| startVMTaskSeq       | integer                                     |     | 该字段只读开机任务序号                                   |
| status               | string                                      |     | 该字段只读容器状态                                     |
| systemElapsed        | integer                                     |     | 该字段只读系统耗时（开机时间）                               |
| template             | string                                      |     |                                               |
| thirdPartyAgentEnvId | string                                      |     | 第三方构建环境ID                                     |
| thirdPartyAgentId    | string                                      |     | 第三方构建Hash ID                                  |
| thirdPartyWorkspace  | string                                      |     | 第三方构建环境工作空间                                   |
| timeCost             | [BuildRecordTimeCost](#BuildRecordTimeCost) |     |                                               |
| tstackAgentId        | string                                      |     | TStack Hash Id                                |
| variables            | Map<String, string>                         |     |                                               |
| vmNames              | List<string>                                | √   | 预指定VM名称列表                                     |

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

#### BuildNo
##### 构建版本号

| 参数名称           | 参数类型                                                             | 必须  | 参数说明 |
| -------------- | ---------------------------------------------------------------- | --- | ---- |
| buildNo        | integer                                                          |     |      |
| buildNoType    | ENUM(CONSISTENT, SUCCESS_BUILD_INCREMENT, EVERY_BUILD_INCREMENT) |     |      |
| currentBuildNo | integer                                                          |     |      |
| required       | boolean                                                          |     |      |

#### BuildFormProperty
##### 构建模型-表单元素属性

| 参数名称                     | 参数类型                                                                                                                                                                                            | 必须  | 参数说明                                 |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------------------------------ |
| cascadeProps             | [BuildCascadeProps](#BuildCascadeProps)                                                                                                                                                         |     |                                      |
| category                 | string                                                                                                                                                                                          |     | 分组信息                                 |
| constant                 | boolean                                                                                                                                                                                         |     | 是否为常量                                |
| containerType            | [BuildContainerType](#BuildContainerType)                                                                                                                                                       |     |                                      |
| defaultValue             | Any                                                                                                                                                                                             | √   | 默认值                                  |
| desc                     | string                                                                                                                                                                                          |     | 描述                                   |
| displayCondition         | Map<String, string>                                                                                                                                                                             |     | 展示条件                                 |
| enableVersionControl     | boolean                                                                                                                                                                                         |     | 开启文件版本管理                             |
| glob                     | string                                                                                                                                                                                          |     | 自定义仓库通配符                             |
| id                       | string                                                                                                                                                                                          | √   | 元素ID-标识符                             |
| label                    | string                                                                                                                                                                                          |     | 元素标签                                 |
| latestRandomStringInPath | string                                                                                                                                                                                          |     | 最新的目录随机字符串                           |
| name                     | string                                                                                                                                                                                          |     | 元素名称                                 |
| options                  | List<[BuildFormValue](#BuildFormValue)>                                                                                                                                                         |     | 下拉框列表                                |
| payload                  | Any                                                                                                                                                                                             |     | 页面所需内容，后台仅保存，不做处理                    |
| placeholder              | string                                                                                                                                                                                          |     | 元素placeholder                        |
| properties               | Map<String, string>                                                                                                                                                                             |     | 文件元数据                                |
| propertyType             | string                                                                                                                                                                                          |     | 元素模块                                 |
| randomStringInPath       | string                                                                                                                                                                                          |     | 目录随机字符串                              |
| readOnly                 | boolean                                                                                                                                                                                         |     | 是否只读                                 |
| relativePath             | string                                                                                                                                                                                          |     | relativePath                         |
| replaceKey               | string                                                                                                                                                                                          |     | 替换搜索url中的搜素关键字                       |
| repoHashId               | string                                                                                                                                                                                          |     | repoHashId                           |
| required                 | boolean                                                                                                                                                                                         | √   | 是否必须（新前端的入参标识）                       |
| scmType                  | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4)                                                                                                     |     | 代码库类型下拉                              |
| searchUrl                | string                                                                                                                                                                                          |     | 搜索url, 当是下拉框选项时，列表值从url获取不再从option获取 |
| type                     | ENUM(string, textarea, enum, date, long, boolean, svn_tag, git_ref, repo_ref, multiple, code_lib, container_type, artifactory, sub_pipeline, custom_file, password, do not storage in database) | √   | 元素类型                                 |
| value                    | Any                                                                                                                                                                                             |     | 上次构建的取值                              |
| valueNotEmpty            | boolean                                                                                                                                                                                         |     | 参数值是否必填                              |

#### BuildCascadeProps
##### 构建模型-表单元素属性

| 参数名称       | 参数类型                                    | 必须  | 参数说明 |
| ---------- | --------------------------------------- | --- | ---- |
| children   | [BuildCascadeProps](#BuildCascadeProps) |     |      |
| id         | string                                  | √   |      |
| options    | List<[BuildFormValue](#BuildFormValue)> | √   |      |
| replaceKey | string                                  |     |      |
| searchUrl  | string                                  |     |      |

#### BuildFormValue
##### 构建模型-下拉框表单元素值

| 参数名称  | 参数类型   | 必须  | 参数说明      |
| ----- | ------ | --- | --------- |
| key   | string | √   | 元素值ID-标识符 |
| value | string | √   | 元素值名称-显示用 |

#### BuildContainerType
##### 构建机类型下拉

| 参数名称      | 参数类型                                                                                                                                                                                        | 必须  | 参数说明 |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ---- |
| buildType | ENUM(ESXi, MACOS, WINDOWS, KUBERNETES, PUBLIC_DEVCLOUD, PUBLIC_BCS, THIRD_PARTY_AGENT_ID, THIRD_PARTY_AGENT_ENV, THIRD_PARTY_PCG, THIRD_PARTY_DEVCLOUD, GIT_CI, DOCKER, STREAM, AGENT_LESS) |     |      |
| os        | ENUM(MACOS, WINDOWS, LINUX)                                                                                                                                                                 |     |      |

#### JobControlOption
##### job流程控制模型

| 参数名称                       | 参数类型                                                                                                                                                                    | 必须  | 参数说明                                            |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ----------------------------------------------- |
| allNodeConcurrency         | integer                                                                                                                                                                 |     | 第三方构建机集群-所有节点并发限制                               |
| continueWhenFailed         | boolean                                                                                                                                                                 |     | 是否失败继续                                          |
| customCondition            | string                                                                                                                                                                  |     | 自定义条件                                           |
| customVariables            | List<[NameAndValue](#NameAndValue)>                                                                                                                                     |     | 自定义变量                                           |
| dependOnContainerId2JobIds | Map<String, string>                                                                                                                                                     |     | containerId与jobId映射，depend on运行时使用的是containerId |
| dependOnId                 | List<string>                                                                                                                                                            |     | 需要过滤不存在的job，定义为var类型                            |
| dependOnName               | string                                                                                                                                                                  |     | job依赖名称                                         |
| dependOnType               | ENUM(ID, NAME)                                                                                                                                                          |     | job依赖                                           |
| enable                     | boolean                                                                                                                                                                 | √   | 是否启用Job                                         |
| prepareTimeout             | integer                                                                                                                                                                 |     | Job准备环境的超时时间 分钟Minutes                          |
| runCondition               | ENUM(STAGE_RUNNING, CUSTOM_VARIABLE_MATCH, CUSTOM_VARIABLE_MATCH_NOT_RUN, CUSTOM_CONDITION_MATCH, PREVIOUS_STAGE_SUCCESS, PREVIOUS_STAGE_FAILED, PREVIOUS_STAGE_CANCEL) | √   | 运行条件                                            |
| singleNodeConcurrency      | integer                                                                                                                                                                 |     | 第三方构建机集群-单节点并发限制                                |
| timeout                    | integer                                                                                                                                                                 |     | ob执行的超时时间 分钟Minutes                             |
| timeoutVar                 | string                                                                                                                                                                  |     | 新的Job执行的超时时间，支持变量 分钟Minutes，出错则取timeout的值       |

#### MatrixControlOption
##### 构建矩阵配置项模型

| 参数名称               | 参数类型                          | 必须  | 参数说明                  |
| ------------------ | ----------------------------- | --- | --------------------- |
| customDispatchInfo | [DispatchInfo](#DispatchInfo) |     |                       |
| excludeCaseStr     | string                        |     | 排除的参数组合（变量名到特殊值映射的数组） |
| fastKill           | boolean                       |     | 是否启用容器失败快速终止整个矩阵      |
| finishCount        | integer                       |     | 完成执行的数量               |
| includeCaseStr     | string                        |     | 额外的参数组合（变量名到特殊值映射的数组） |
| maxConcurrency     | integer                       |     | Job运行的最大并发量           |
| strategyStr        | string                        |     | 分裂策略（支持变量、Json、参数映射表） |
| totalCount         | integer                       |     | 矩阵组的总数量               |

#### DispatchInfo
##### 自定义调度类型（用于生成DispatchType的任意对象）

| 参数名称         | 参数类型   | 必须  | 参数说明 |
| ------------ | ------ | --- | ---- |
| dispatchInfo | string | √   |      |
| name         | string |     |      |

#### MutexGroup
##### 互斥组模型

| 参数名称              | 参数类型    | 必须  | 参数说明                              |
| ----------------- | ------- | --- | --------------------------------- |
| enable            | boolean | √   | 是否启用                              |
| linkTip           | string  |     | 占用锁定的信息用于日志提示                     |
| mutexGroupName    | string  |     | 互斥组名称                             |
| queue             | integer | √   | 排队队列大小                            |
| queueEnable       | boolean | √   | 是否排队                              |
| runtimeMutexGroup | string  |     | 运行时实际互斥锁名称（有值则已初始化）               |
| timeout           | integer | √   | 排队等待时间（分钟）0表示不等待直接失败              |
| timeoutVar        | string  |     | 支持变量解析的timeout，变量值非数字则会改取timeout值 |

#### DispatchType
##### 新的选择构建机环境

| 参数名称      | 参数类型   | 必须  | 参数说明                                                                                                                                                               |
| --------- | ------ | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| buildType | string | √   | 用于指定实现某一多态类, 可选[DockerDispatchType, KubernetesDispatchType, ThirdPartyAgentIDDispatchType, ThirdPartyAgentEnvDispatchType, ThirdPartyDevCloudDispatchType],具体实现见下方 |
| value     | string |     |                                                                                                                                                                    |

#### DockerDispatchType
 *多态基类 <DispatchType> 的实现处, 其中当字段 buildType = [DOCKER] 时指定为该类实现*
 

| 参数名称                    | 参数类型                           | 必须  | 参数说明         |
| ----------------------- | ------------------------------ | --- | ------------ |
| buildType               | string                         | 必须是 | 多态类实现        | DOCKER |
| credentialId            | string                         |     | 凭证id         |
| credentialProject       | string                         |     | 凭证项目id       |
| dockerBuildVersion      | string                         |     | docker构建版本   |
| imageCode               | string                         |     | 商店镜像代码       |
| imageName               | string                         |     | 商店镜像名称       |
| imagePublicFlag         | boolean                        |     | 商店镜像公共标识     |
| imageRDType             | string                         |     | 商店镜像研发来源c    |
| imageRepositoryPassword | string                         |     | 镜像仓库密码       |
| imageRepositoryUserName | string                         |     | 镜像仓库用户名      |
| imageType               | ENUM(BKDEVOPS, BKSTORE, THIRD) |     | 镜像类型         |
| imageVersion            | string                         |     | 商店镜像版本       |
| performanceConfigId     | integer                        |     | docker资源配置ID |
| recommendFlag           | boolean                        |     | 商店镜像是否推荐     |
| value                   | string                         |     |              |

#### KubernetesDispatchType
 *多态基类 <DispatchType> 的实现处, 其中当字段 buildType = [KUBERNETES] 时指定为该类实现*
 

| 参数名称                   | 参数类型                           | 必须  | 参数说明       |
| ---------------------- | ------------------------------ | --- | ---------- |
| buildType              | string                         | 必须是 | 多态类实现      | KUBERNETES |
| credentialId           | string                         |     | 凭证id       |
| credentialProject      | string                         |     | 凭证项目id     |
| dockerBuildVersion     | string                         |     | docker构建版本 |
| imageCode              | string                         |     | 商店镜像代码     |
| imageName              | string                         |     | 商店镜像名称     |
| imagePublicFlag        | boolean                        |     | 商店镜像公共标识   |
| imageRDType            | string                         |     | 商店镜像研发来源c  |
| imageType              | ENUM(BKDEVOPS, BKSTORE, THIRD) |     | 镜像类型       |
| imageVersion           | string                         |     | 商店镜像版本     |
| kubernetesBuildVersion | string                         |     |            |
| performanceConfigId    | integer                        |     |            |
| recommendFlag          | boolean                        |     | 商店镜像是否推荐   |
| value                  | string                         |     |            |

#### ThirdPartyAgentIDDispatchType
 *多态基类 <DispatchType> 的实现处, 其中当字段 buildType = [THIRD_PARTY_AGENT_ID] 时指定为该类实现*
 

| 参数名称        | 参数类型                                                    | 必须  | 参数说明  |
| ----------- | ------------------------------------------------------- | --- | ----- |
| buildType   | string                                                  | 必须是 | 多态类实现 | THIRD_PARTY_AGENT_ID |
| agentType   | ENUM(ID, NAME, REUSE_JOB_ID)                            |     |       |
| displayName | string                                                  |     |       |
| dockerInfo  | [ThirdPartyAgentDockerInfo](#ThirdPartyAgentDockerInfo) |     |       |
| env         | boolean                                                 |     |       |
| reusedInfo  | [ReusedInfo](#ReusedInfo)                               |     |       |
| single      | boolean                                                 |     |       |
| value       | string                                                  |     |       |
| workspace   | string                                                  |     |       |

#### ThirdPartyAgentEnvDispatchType
 *多态基类 <DispatchType> 的实现处, 其中当字段 buildType = [THIRD_PARTY_AGENT_ENV] 时指定为该类实现*
 

| 参数名称         | 参数类型                                                    | 必须  | 参数说明  |
| ------------ | ------------------------------------------------------- | --- | ----- |
| buildType    | string                                                  | 必须是 | 多态类实现 | THIRD_PARTY_AGENT_ENV |
| agentType    | ENUM(ID, NAME, REUSE_JOB_ID)                            |     |       |
| dockerInfo   | [ThirdPartyAgentDockerInfo](#ThirdPartyAgentDockerInfo) |     |       |
| env          | boolean                                                 |     |       |
| envName      | string                                                  |     |       |
| envProjectId | string                                                  |     |       |
| reusedInfo   | [ReusedInfo](#ReusedInfo)                               |     |       |
| single       | boolean                                                 |     |       |
| value        | string                                                  |     |       |
| workspace    | string                                                  |     |       |

#### ThirdPartyDevCloudDispatchType
 *多态基类 <DispatchType> 的实现处, 其中当字段 buildType = [THIRD_PARTY_DEVCLOUD] 时指定为该类实现*
 

| 参数名称        | 参数类型                         | 必须  | 参数说明    |
| ----------- | ---------------------------- | --- | ------- |
| buildType   | string                       | 必须是 | 多态类实现   | THIRD_PARTY_DEVCLOUD |
| agentType   | ENUM(ID, NAME, REUSE_JOB_ID) |     | agent类型 |
| displayName | string                       |     | 展示名称    |
| value       | string                       |     |         |
| workspace   | string                       |     | 工作空间    |

#### ThirdPartyAgentDockerInfo
##### 

| 参数名称            | 参数类型                                                                        | 必须  | 参数说明 |
| --------------- | --------------------------------------------------------------------------- | --- | ---- |
| credential      | [Credential](#Credential)                                                   |     |      |
| image           | string                                                                      |     |      |
| imagePullPolicy | string                                                                      |     |      |
| imageType       | ENUM(BKDEVOPS, BKSTORE, THIRD)                                              |     |      |
| options         | [DockerOptions](#DockerOptions)                                             |     |      |
| storeImage      | [ThirdPartyAgentDockerInfoStoreImage](#ThirdPartyAgentDockerInfoStoreImage) |     |      |

#### Credential
##### 

| 参数名称                | 参数类型   | 必须  | 参数说明 |
| ------------------- | ------ | --- | ---- |
| acrossTemplateId    | string |     |      |
| credentialId        | string |     |      |
| credentialProjectId | string |     |      |
| jobId               | string |     |      |
| password            | string |     |      |
| user                | string |     |      |

#### DockerOptions
##### 

| 参数名称       | 参数类型         | 必须  | 参数说明 |
| ---------- | ------------ | --- | ---- |
| gpus       | string       |     |      |
| mounts     | List<string> |     |      |
| privileged | boolean      |     |      |
| volumes    | List<string> |     |      |

#### ThirdPartyAgentDockerInfoStoreImage
##### 

| 参数名称         | 参数类型   | 必须  | 参数说明 |
| ------------ | ------ | --- | ---- |
| imageCode    | string |     |      |
| imageName    | string |     |      |
| imageVersion | string |     |      |

#### ReusedInfo
##### 

| 参数名称      | 参数类型                         | 必须  | 参数说明 |
| --------- | ---------------------------- | --- | ---- |
| agentType | ENUM(ID, NAME, REUSE_JOB_ID) |     |      |
| jobId     | string                       |     |      |
| value     | string                       |     |      |

#### StageControlOption
##### 阶段流程控制模型

| 参数名称            | 参数类型                                                                                                    | 必须  | 参数说明         |
| --------------- | ------------------------------------------------------------------------------------------------------- | --- | ------------ |
| customCondition | string                                                                                                  |     | 自定义条件        |
| customVariables | List<[NameAndValue](#NameAndValue)>                                                                     |     | 自定义变量        |
| enable          | boolean                                                                                                 | √   | 是否启用该阶段      |
| manualTrigger   | boolean                                                                                                 |     | 是否人工触发       |
| reviewDesc      | string                                                                                                  |     | 审核说明         |
| reviewParams    | List<[ManualReviewParam](#ManualReviewParam)>                                                           |     | 审核变量         |
| runCondition    | ENUM(AFTER_LAST_FINISHED, CUSTOM_VARIABLE_MATCH, CUSTOM_VARIABLE_MATCH_NOT_RUN, CUSTOM_CONDITION_MATCH) | √   | 运行条件         |
| timeout         | integer                                                                                                 |     | 等待审核的超时时间    |
| triggerUsers    | List<string>                                                                                            |     | 可触发用户，支持引用变量 |
| triggered       | boolean                                                                                                 |     | 已通过审核        |

#### BuildRecordInfo
##### 执行次数记录

| 参数名称      | 参数类型                                        | 必须  | 参数说明 |
| --------- | ------------------------------------------- | --- | ---- |
| startUser | string                                      | √   | 触发人  |
| timeCost  | [BuildRecordTimeCost](#BuildRecordTimeCost) |     |      |

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

 
