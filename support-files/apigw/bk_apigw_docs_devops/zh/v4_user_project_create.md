### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/project/project_create
### 资源描述
#### 创建项目
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称                  | 参数类型   | 必须  | 参数说明             |
| --------------------- | ------ | --- | ---------------- |
| Content-Type          | string | √   | application/json |
| X-DEVOPS-UID          | string | √   | 用户名              |
| X-DEVOPS-ACCESS-TOKEN | String |     | access_token     |

#### Body参数

| 参数名称 | 参数类型                                    | 必须   |
| ---- | --------------------------------------- | ---- |
| 项目信息 | [ProjectCreateInfo](#ProjectCreateInfo) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' \
-H 'X-DEVOPS-ACCESS-TOKEN: access_token' 
```

### POST 请求样例

```Json
{
  "authSecrecy" : 0,
  "bgId" : 0,
  "bgName" : "",
  "businessLineId" : 0,
  "businessLineName" : "",
  "centerId" : 0,
  "centerName" : "",
  "deptId" : 0,
  "deptName" : "",
  "description" : "",
  "enabled" : false,
  "englishName" : "",
  "kind" : 0,
  "logoAddress" : "",
  "productId" : 0,
  "productName" : "",
  "projectName" : "",
  "projectType" : 0,
  "properties" : {
    "buildMetrics" : false,
    "cloudDesktopNum" : 0,
    "dataTag" : "",
    "disableWhenInactive" : false,
    "enablePipelineNameTips" : false,
    "enableTemplatePermissionManage" : false,
    "loggingLineLimit" : 0,
    "pipelineAsCodeSettings" : {
      "enable" : false,
      "inheritedDialect" : false,
      "pipelineDialect" : "",
      "projectDialect" : ""
    },
    "pipelineDialect" : "",
    "pipelineListPermissionControl" : false,
    "pipelineNameFormat" : "",
    "pluginDetailsDisplayOrder" : [ "enum" ],
    "remotedev" : false,
    "remotedevManager" : ""
  },
  "secrecy" : false,
  "subjectScopes" : [ {
    "fullName" : "",
    "full_name" : "",
    "id" : "",
    "name" : "",
    "type" : "",
    "username" : ""
  } ]
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
#### ProjectCreateInfo
##### 项目-新增模型

| 参数名称             | 参数类型                                        | 必须  | 参数说明        |
| ---------------- | ------------------------------------------- | --- | ----------- |
| authSecrecy      | integer                                     |     | 项目性质        |
| bgId             | integer                                     | √   | BGID        |
| bgName           | string                                      | √   | BG名称        |
| businessLineId   | integer                                     |     | 业务线ID       |
| businessLineName | string                                      |     | 业务线名称       |
| centerId         | integer                                     | √   | 中心ID        |
| centerName       | string                                      | √   | 中心名称        |
| deptId           | integer                                     | √   | 部门ID        |
| deptName         | string                                      | √   | 部门名称        |
| description      | string                                      | √   | 描述          |
| enabled          | boolean                                     | √   | 是否可用        |
| englishName      | string                                      | √   | 英文缩写        |
| kind             | integer                                     | √   | kind        |
| logoAddress      | string                                      |     | logo地址      |
| productId        | integer                                     |     | 运营产品ID      |
| productName      | string                                      |     | 运营产品名称      |
| projectName      | string                                      | √   | 项目名称        |
| projectType      | integer                                     | √   | 项目类型        |
| properties       | [ProjectProperties](#ProjectProperties)     |     |             |
| secrecy          | boolean                                     | √   | 是否保密        |
| subjectScopes    | List<[SubjectScopeInfo](#SubjectScopeInfo)> |     | 项目最大可授权人员范围 |

#### ProjectProperties
##### 项目其他配置

| 参数名称                           | 参数类型                                              | 必须  | 参数说明                     |
| ------------------------------ | ------------------------------------------------- | --- | ------------------------ |
| buildMetrics                   | boolean                                           |     | 该项目是否开启流水线可观测数据          |
| cloudDesktopNum                | integer                                           | √   | 可申请的云桌面数                 |
| dataTag                        | string                                            |     | 数据标签，创建项目时会为该项目分配指定标签的db |
| disableWhenInactive            | boolean                                           |     | 当项目不活跃时，是否禁用             |
| enablePipelineNameTips         | boolean                                           |     | 是否开启流水线命名提示              |
| enableTemplatePermissionManage | boolean                                           |     | 是否开启流水线模板管理              |
| loggingLineLimit               | integer                                           |     | 构建日志归档阈值(单位:万)           |
| pipelineAsCodeSettings         | [PipelineAsCodeSettings](#PipelineAsCodeSettings) | √   |                          |
| pipelineDialect                | string                                            |     | 流水线语法风格                  |
| pipelineListPermissionControl  | boolean                                           |     | 是否控制流水线列表权限              |
| pipelineNameFormat             | string                                            |     | 流水线命名格式                  |
| pluginDetailsDisplayOrder      | List<ENUM(LOG, ARTIFACT, CONFIG)>                 |     | 插件详情展示顺序                 |
| remotedev                      | boolean                                           |     | 是否启用云研发                  |
| remotedevManager               | string                                            |     | 云研发管理员，多人用分号分隔           |

#### PipelineAsCodeSettings
##### 设置-YAML流水线功能设置

| 参数名称             | 参数类型    | 必须  | 参数说明          |
| ---------------- | ------- | --- | ------------- |
| enable           | boolean | √   | 是否支持YAML流水线功能 |
| inheritedDialect | boolean |     | 是否继承项目流水线语言风格 |
| pipelineDialect  | string  |     | 流水线语言风格       |
| projectDialect   | string  |     | 项目级流水线语法风格    |

#### SubjectScopeInfo
##### 授权范围

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| fullName  | string |     |      |
| full_name | string |     |      |
| id        | string |     | ID   |
| name      | string | √   | name |
| type      | string |     | 类型   |
| username  | string |     | 用户名  |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
