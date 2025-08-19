### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/project/{projectId}
### 资源描述
#### 获取项目信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID英文名标识  |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称                  | 参数类型   | 必须  | 参数说明             |
| --------------------- | ------ | --- | ---------------- |
| Content-Type          | string | √   | application/json |
| X-DEVOPS-UID          | string | √   | 用户名              |
| X-DEVOPS-ACCESS-TOKEN | String |     | access_token     |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                | 说明               |
| ------- | ----------------------------------- | ---------------- |
| default | [ResultProjectVO](#ResultProjectVO) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' \
-H 'X-DEVOPS-ACCESS-TOKEN: access_token' 
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : {
    "approvalMsg" : "",
    "approvalStatus" : 0,
    "approvalTime" : "",
    "approver" : "",
    "authSecrecy" : 0,
    "bgId" : "",
    "bgName" : "",
    "businessLineId" : "",
    "businessLineName" : "",
    "canView" : false,
    "ccAppId" : 0,
    "ccAppName" : "",
    "cc_app_id" : 0,
    "cc_app_name" : "",
    "centerId" : "",
    "centerName" : "",
    "channelCode" : "",
    "createdAt" : "",
    "creator" : "",
    "dataId" : 0,
    "deployType" : "",
    "deptId" : "",
    "deptName" : "",
    "description" : "",
    "enableExternal" : false,
    "enableIdc" : false,
    "enabled" : false,
    "englishName" : "",
    "extra" : "",
    "gray" : false,
    "helmChartEnabled" : false,
    "hybridCcAppId" : 0,
    "hybrid_cc_app_id" : 0,
    "id" : 0,
    "kind" : 0,
    "logoAddr" : "",
    "managePermission" : false,
    "offlined" : false,
    "pipelineLimit" : 0,
    "pipelineTemplateInstallPerm" : false,
    "productId" : 0,
    "productName" : "",
    "projectCode" : "",
    "projectId" : "",
    "projectName" : "",
    "projectType" : 0,
    "project_code" : "",
    "project_id" : "",
    "project_name" : "",
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
    "relationId" : "",
    "remark" : "",
    "routerTag" : "",
    "secrecy" : false,
    "showUserManageIcon" : false,
    "subjectScopes" : [ {
      "fullName" : "",
      "full_name" : "",
      "id" : "",
      "name" : "",
      "type" : "",
      "username" : ""
    } ],
    "tipsStatus" : 0,
    "updatedAt" : "",
    "updator" : "",
    "useBk" : false
  },
  "message" : "",
  "requestId" : "",
  "request_id" : "",
  "result" : false
}
```

### 相关模型数据
#### ResultProjectVO
##### 数据返回包装模型

| 参数名称       | 参数类型                    | 必须  | 参数说明 |
| ---------- | ----------------------- | --- | ---- |
| code       | integer                 | √   | 状态码  |
| data       | [ProjectVO](#ProjectVO) |     |      |
| message    | string                  |     | 错误信息 |
| requestId  | string                  |     | 请求ID |
| request_id | string                  |     |      |
| result     | boolean                 |     | 请求结果 |

#### ProjectVO
##### 项目-显示模型

| 参数名称                        | 参数类型                                        | 必须  | 参数说明                                               |
| --------------------------- | ------------------------------------------- | --- | -------------------------------------------------- |
| approvalMsg                 | string                                      |     | 项目审批message                                        |
| approvalStatus              | integer                                     |     | 审批状态                                               |
| approvalTime                | string                                      |     | 审批时间                                               |
| approver                    | string                                      |     | 审批人                                                |
| authSecrecy                 | integer                                     |     | 是否权限私密                                             |
| bgId                        | string                                      |     | 事业群ID                                              |
| bgName                      | string                                      |     | 事业群名字                                              |
| businessLineId              | string                                      |     | 业务线ID                                              |
| businessLineName            | string                                      |     | 业务线名称                                              |
| canView                     | boolean                                     |     | 是否可以查看                                             |
| ccAppId                     | integer                                     |     | cc业务ID                                             |
| ccAppName                   | string                                      |     | cc业务名称                                             |
| cc_app_id                   | integer                                     |     | 旧版cc业务ID(即将作废，兼容插件中被引用到的旧的字段命名，请用ccAppId代替)        |
| cc_app_name                 | string                                      |     | 旧版cc业务名称(即将作废，兼容插件中被引用到的旧的字段命名，请用ccAppName代替)      |
| centerId                    | string                                      |     | 中心ID                                               |
| centerName                  | string                                      |     | 中心名称                                               |
| channelCode                 | string                                      |     | 渠道                                                 |
| createdAt                   | string                                      |     | 创建时间                                               |
| creator                     | string                                      |     | 创建人                                                |
| dataId                      | integer                                     |     | 数据ID                                               |
| deployType                  | string                                      |     | 部署类型                                               |
| deptId                      | string                                      |     | 部门ID                                               |
| deptName                    | string                                      |     | 部门名称                                               |
| description                 | string                                      |     | 描述                                                 |
| enableExternal              | boolean                                     |     | 支持构建机访问外网                                          |
| enableIdc                   | boolean                                     |     | 支持IDC构建机                                           |
| enabled                     | boolean                                     |     | 启用                                                 |
| englishName                 | string                                      | √   | 英文缩写                                               |
| extra                       | string                                      |     | extra                                              |
| gray                        | boolean                                     | √   | 是否灰度                                               |
| helmChartEnabled            | boolean                                     |     | 是否启用图表激活                                           |
| hybridCcAppId               | integer                                     |     | 混合云CC业务ID                                          |
| hybrid_cc_app_id            | integer                                     |     | 混合云CC业务ID(即将作废，兼容插件中被引用到的旧的字段命名，请用hybridCcAppId代替) |
| id                          | integer                                     | √   | 主键ID                                               |
| kind                        | integer                                     |     | kind                                               |
| logoAddr                    | string                                      |     | logo地址                                             |
| managePermission            | boolean                                     |     | 是否拥有新版权限中心项目管理权限                                   |
| offlined                    | boolean                                     |     | 是否离线                                               |
| pipelineLimit               | integer                                     |     | 流水线数量上限                                            |
| pipelineTemplateInstallPerm | boolean                                     |     | 安装模板权限                                             |
| productId                   | integer                                     |     | 运营产品ID                                             |
| productName                 | string                                      |     | 运营产品名称                                             |
| projectCode                 | string                                      | √   | 项目代码（蓝盾项目Id）                                       |
| projectId                   | string                                      | √   | 项目ID（很少使用）                                         |
| projectName                 | string                                      | √   | 项目名称                                               |
| projectType                 | integer                                     |     | 项目类型                                               |
| project_code                | string                                      |     | 旧版项目代码(即将作废，兼容插件中被引用到的旧的字段命名，请用projectCode代替)      |
| project_id                  | string                                      |     | 项目ID(即将作废，兼容插件中被引用到的旧的字段命名，请用projectId代替)          |
| project_name                | string                                      |     | 旧版项目名称(即将作废，兼容插件中被引用到的旧的字段命名，请用projectName代替)      |
| properties                  | [ProjectProperties](#ProjectProperties)     |     |                                                    |
| relationId                  | string                                      |     | 关联系统Id                                             |
| remark                      | string                                      |     | 评论                                                 |
| routerTag                   | string                                      |     | 项目路由指向                                             |
| secrecy                     | boolean                                     |     | 是否保密                                               |
| showUserManageIcon          | boolean                                     |     | 是否展示用户管理图标                                         |
| subjectScopes               | List<[SubjectScopeInfo](#SubjectScopeInfo)> |     | 项目最大可授权人员范围                                        |
| tipsStatus                  | integer                                     |     | 项目提示状态,0-不展示,1-展示创建成功,2-展示编辑成功                     |
| updatedAt                   | string                                      |     | 修改时间                                               |
| updator                     | string                                      |     | 修改人                                                |
| useBk                       | boolean                                     |     | useBK                                              |

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

 
