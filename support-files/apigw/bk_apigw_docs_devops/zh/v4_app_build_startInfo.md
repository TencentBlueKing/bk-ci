### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/build_manual_startup_info
### 资源描述
#### 获取流水线手动启动参数
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明          |
| ---------- | ------- | --- | ------------- |
| pipelineId | String  | √   | 流水线ID         |
| version    | integer |     | 指定草稿版本（为调试构建） |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                          | 说明               |
| ------- | ------------------------------------------------------------- | ---------------- |
| default | [ResultBuildManualStartupInfo](#ResultBuildManualStartupInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}&version={version}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "buildNo" : {
      "buildNo" : 0,
      "buildNoType" : "enum",
      "currentBuildNo" : 0,
      "required" : false
    },
    "canElementSkip" : false,
    "canManualStartup" : false,
    "properties" : [ {
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
    "useLatestParameters" : false
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultBuildManualStartupInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                                              | 必须  | 参数说明 |
| ------- | ------------------------------------------------- | --- | ---- |
| data    | [BuildManualStartupInfo](#BuildManualStartupInfo) |     |      |
| message | string                                            |     | 错误信息 |
| status  | integer                                           | √   | 状态码  |

#### BuildManualStartupInfo
##### 构建模型-流水线手动启动信息

| 参数名称                | 参数类型                                          | 必须  | 参数说明            |
| ------------------- | --------------------------------------------- | --- | --------------- |
| buildNo             | [BuildNo](#BuildNo)                           |     |                 |
| canElementSkip      | boolean                                       | √   | 是否可跳过插件         |
| canManualStartup    | boolean                                       | √   | 是否可手工启动         |
| properties          | List<[BuildFormProperty](#BuildFormProperty)> | √   | 启动表单元素列表        |
| useLatestParameters | boolean                                       |     | 是否使用最近一次的参数进行构建 |

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

 
