### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/version/create_with_template
### 资源描述
#### 通过指定模板创建流水线
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称      | 参数类型                                                            | 必须   |
| --------- | --------------------------------------------------------------- | ---- |
| 流水线模型实例请求 | [TemplateInstanceCreateRequest](#TemplateInstanceCreateRequest) | true |

#### 响应参数

| HTTP代码  | 参数类型                                                      | 说明               |
| ------- | --------------------------------------------------------- | ---------------- |
| default | [ResultDeployPipelineResult](#ResultDeployPipelineResult) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "emptyTemplate" : false,
  "inheritedDialect" : false,
  "instanceType" : "",
  "labels" : [ "" ],
  "pipelineDialect" : "",
  "pipelineName" : "",
  "staticViews" : [ "" ],
  "templateId" : "",
  "templateVersion" : 0,
  "useConcurrencyGroup" : false,
  "useLabelSettings" : false,
  "useSubscriptionSettings" : false
}
```

### default 返回样例

```Json
{
  "data" : {
    "pipelineId" : "",
    "pipelineName" : "",
    "targetUrl" : "",
    "updateBuildNo" : false,
    "version" : 0,
    "versionName" : "",
    "versionNum" : 0,
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
#### TemplateInstanceCreateRequest
##### 

| 参数名称                    | 参数类型         | 必须  | 参数说明           |
| ----------------------- | ------------ | --- | -------------- |
| emptyTemplate           | boolean      |     | 是否为空模板         |
| inheritedDialect        | boolean      |     | 是否继承项目流水线语言风格  |
| instanceType            | string       |     | 创建实例的模式        |
| labels                  | List<string> |     | 流水线标签          |
| pipelineDialect         | string       |     | 流水线语言风格        |
| pipelineName            | string       | √   | 流水线名称          |
| staticViews             | List<string> |     | 静态流水线组         |
| templateId              | string       | √   | 模板ID           |
| templateVersion         | integer      | √   | 模板版本号（为空时默认最新） |
| useConcurrencyGroup     | boolean      |     | 是否使用并发组配置      |
| useLabelSettings        | boolean      |     | 是否使用标签配置       |
| useSubscriptionSettings | boolean      |     | 是否使用通知配置       |

#### ResultDeployPipelineResult
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | [DeployPipelineResult](#DeployPipelineResult) |     |      |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### DeployPipelineResult
##### 配置流水线结果

| 参数名称          | 参数类型                              | 必须  | 参数说明          |
| ------------- | --------------------------------- | --- | ------------- |
| pipelineId    | string                            | √   | 流水线ID         |
| pipelineName  | string                            | √   | 流水线名称         |
| targetUrl     | string                            |     | 目标链接          |
| updateBuildNo | boolean                           |     | 是否更新了推荐版本号基准值 |
| version       | integer                           | √   | 流水线版本号        |
| versionName   | string                            |     | 生成版本名称        |
| versionNum    | integer                           |     | 发布版本号         |
| yamlInfo      | [PipelineYamlVo](#PipelineYamlVo) |     |               |

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

 
