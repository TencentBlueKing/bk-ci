### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/templates
### 资源描述
#### 模版管理-获取模版列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称         | 参数类型                                | 必须  | 参数说明              |
| ------------ | ----------------------------------- | --- | ----------------- |
| orderBy      | ENUM(NAME, CREATOR, CREATE_TIME)    |     | 模版排序字段            |
| page         | integer                             | √   | 页码                |
| pageSize     | integer                             | √   | 每页条数(默认20, 最大100) |
| sort         | ENUM(ASC, DESC)                     |     | orderBy排序顺序       |
| storeFlag    | boolean                             |     | 是否已关联到store       |
| templateType | ENUM(CUSTOMIZE, CONSTRAINT, PUBLIC) |     | 模版类型              |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                | 说明               |
| ------- | --------------------------------------------------- | ---------------- |
| default | [ResultTemplateListModel](#ResultTemplateListModel) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?orderBy={orderBy}&page={page}&pageSize={pageSize}&sort={sort}&storeFlag={storeFlag}&templateType={templateType}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "enableTemplatePermissionManage" : false,
    "hasCreatePermission" : false,
    "hasPermission" : false,
    "models" : [ {
      "associateCodes" : [ "" ],
      "associatePipelines" : [ {
        "id" : ""
      } ],
      "canDelete" : false,
      "canEdit" : false,
      "canView" : false,
      "creator" : "",
      "hasInstance2Upgrade" : false,
      "hasPermission" : false,
      "logoUrl" : "",
      "name" : "",
      "storeFlag" : false,
      "templateId" : "",
      "templateType" : "",
      "templateTypeDesc" : "",
      "updateTime" : 0,
      "version" : 0,
      "versionName" : ""
    } ],
    "projectId" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultTemplateListModel
##### 数据返回包装模型

| 参数名称    | 参数类型                                    | 必须  | 参数说明 |
| ------- | --------------------------------------- | --- | ---- |
| data    | [TemplateListModel](#TemplateListModel) |     |      |
| message | string                                  |     | 错误信息 |
| status  | integer                                 | √   | 状态码  |

#### TemplateListModel
##### 数据

| 参数名称                           | 参数类型                                  | 必须  | 参数说明      |
| ------------------------------ | ------------------------------------- | --- | --------- |
| count                          | integer                               |     | 数量        |
| enableTemplatePermissionManage | boolean                               |     | 是否开启模板权限  |
| hasCreatePermission            | boolean                               |     | 是否有创建模板权限 |
| hasPermission                  | boolean                               |     | 是否有操作权限   |
| models                         | List<[TemplateModel](#TemplateModel)> |     | 模型        |
| projectId                      | string                                |     | 项目id      |

#### TemplateModel
##### 模板模型

| 参数名称                | 参数类型                            | 必须  | 参数说明      |
| ------------------- | ------------------------------- | --- | --------- |
| associateCodes      | List<string>                    | √   | 关联的代码库    |
| associatePipelines  | List<[PipelineId](#PipelineId)> | √   | 关联的流水线    |
| canDelete           | boolean                         |     | 是否有模版删除权限 |
| canEdit             | boolean                         |     | 是否有模版编辑权限 |
| canView             | boolean                         |     | 是否有模版查看权限 |
| creator             | string                          | √   | 创建者       |
| hasInstance2Upgrade | boolean                         | √   | 是否有可更新实例  |
| hasPermission       | boolean                         | √   | 是否有模版操作权限 |
| logoUrl             | string                          | √   | 模版logo    |
| name                | string                          | √   | 模版名称      |
| storeFlag           | boolean                         | √   | 是否关联到市场   |
| templateId          | string                          | √   | 模版ID      |
| templateType        | string                          | √   | 模板类型      |
| templateTypeDesc    | string                          | √   | 模板类型描述    |
| updateTime          | integer                         | √   | 更新时间      |
| version             | integer                         | √   | 版本ID      |
| versionName         | string                          | √   | 最新版本号     |

#### PipelineId
##### 流水线模型-ID

| 参数名称 | 参数类型   | 必须  | 参数说明  |
| ---- | ------ | --- | ----- |
| id   | string | √   | 流水线ID |

 
