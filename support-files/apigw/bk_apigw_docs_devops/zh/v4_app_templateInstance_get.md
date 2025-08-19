### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/templates/templateInstances
### 资源描述
#### 获取流水线模板的实例列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型                                              | 必须  | 参数说明              |
| ---------- | ------------------------------------------------- | --- | ----------------- |
| desc       | boolean                                           |     | 是否降序              |
| page       | integer                                           |     | 第几页               |
| pageSize   | integer                                           |     | 每页条数(默认20, 最大100) |
| searchKey  | String                                            |     | 名字搜索的关键字          |
| sortType   | ENUM(PIPELINE_NAME, VERSION, UPDATE_TIME, STATUS) |     | 排序字段              |
| templateId | String                                            | √   | 模板ID              |

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
| default | [ResultTemplateInstancePage](#ResultTemplateInstancePage) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?desc={desc}&page={page}&pageSize={pageSize}&searchKey={searchKey}&sortType={sortType}&templateId={templateId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "hasCreateTemplateInstancePerm" : false,
    "instances" : [ {
      "hasPermission" : false,
      "instanceErrorInfo" : "",
      "pipelineId" : "",
      "pipelineName" : "",
      "status" : "enum",
      "templateId" : "",
      "updateTime" : 0,
      "version" : 0,
      "versionName" : ""
    } ],
    "latestVersion" : {
      "creator" : "",
      "updateTime" : 0,
      "version" : 0,
      "versionName" : ""
    },
    "page" : 0,
    "pageSize" : 0,
    "projectId" : "",
    "templateId" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultTemplateInstancePage
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | [TemplateInstancePage](#TemplateInstancePage) |     |      |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### TemplateInstancePage
##### 数据

| 参数名称                          | 参数类型                                        | 必须  | 参数说明         |
| ----------------------------- | ------------------------------------------- | --- | ------------ |
| count                         | integer                                     |     | 数量           |
| hasCreateTemplateInstancePerm | boolean                                     |     | 是否有创建模板实例权限  |
| instances                     | List<[TemplatePipeline](#TemplatePipeline)> |     | 模板生成的流水线实例列表 |
| latestVersion                 | [TemplateVersion](#TemplateVersion)         |     |              |
| page                          | integer                                     |     | 页数           |
| pageSize                      | integer                                     |     | 每页数量         |
| projectId                     | string                                      |     | 项目id         |
| templateId                    | string                                      |     | 模板id         |

#### TemplatePipeline
##### 模板生成的流水线实例列表

| 参数名称              | 参数类型                                            | 必须  | 参数说明      |
| ----------------- | ----------------------------------------------- | --- | --------- |
| hasPermission     | boolean                                         |     | 是否有编辑权限   |
| instanceErrorInfo | string                                          |     | 模板实例化错误信息 |
| pipelineId        | string                                          |     | 流水线id     |
| pipelineName      | string                                          |     | 流水线名称     |
| status            | ENUM(PENDING_UPDATE, UPDATING, UPDATED, FAILED) |     | 流水线模板状态   |
| templateId        | string                                          |     | 模板id      |
| updateTime        | integer                                         |     | 更新时间      |
| version           | integer                                         |     | 版本        |
| versionName       | string                                          |     | 版本名称      |

#### TemplateVersion
##### 模板版本信息

| 参数名称        | 参数类型    | 必须  | 参数说明 |
| ----------- | ------- | --- | ---- |
| creator     | string  | √   | 构建者  |
| updateTime  | integer | √   | 更新时间 |
| version     | integer | √   | 版本号  |
| versionName | string  | √   | 版本名称 |

 
