### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/pipelineView
### 资源描述
#### 添加视图(流水线组)
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   | 项目ID |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称      | 参数类型                                  | 必须  |
| --------- | ------------------------------------- | --- |
| 流水线视图创建模型 | [PipelineViewForm](#PipelineViewForm) |     |

#### 响应参数

| HTTP代码  | 参数类型                                          | 说明               |
| ------- | --------------------------------------------- | ---------------- |
| default | [ResultPipelineViewId](#ResultPipelineViewId) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "filters" : [ {
    "@type" : ""
  } ],
  "id" : "",
  "logic" : "enum",
  "name" : "",
  "pipelineIds" : [ "" ],
  "projected" : false,
  "viewType" : 0
}
```

### default 返回样例

```Json
{
  "data" : {
    "id" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### PipelineViewForm
##### 流水线视图表单

| 参数名称        | 参数类型                                            | 必须  | 参数说明               |
| ----------- | ----------------------------------------------- | --- | ------------------ |
| filters     | List<[PipelineViewFilter](#PipelineViewFilter)> | √   | 流水线视图过滤器列表         |
| id          | string                                          |     | ID                 |
| logic       | ENUM(AND, OR)                                   | √   | 逻辑符                |
| name        | string                                          | √   | 视图名称               |
| pipelineIds | List<string>                                    |     | 流水线列表              |
| projected   | boolean                                         | √   | 是否项目               |
| viewType    | integer                                         | √   | 流水线组类型,1--动态,2--静态 |

#### PipelineViewFilter
##### 流水线视图过滤器列表

| 参数名称  | 参数类型   | 必须  | 参数说明                                                                                                                                   |
| ----- | ------ | --- | -------------------------------------------------------------------------------------------------------------------------------------- |
| @type | string | √   | 用于指定实现某一多态类, 可选[PipelineViewFilterByName, PipelineViewFilterByCreator, PipelineViewFilterByLabel, PipelineViewFilterByPacRepo],具体实现见下方 |

#### PipelineViewFilterByName
 *多态基类 <PipelineViewFilter> 的实现处, 其中当字段 @type = [filterByName] 时指定为该类实现*
 

| 参数名称         | 参数类型                                                         | 必须  | 参数说明  |
| ------------ | ------------------------------------------------------------ | --- | ----- |
| @type        | string                                                       | 必须是 | 多态类实现 | filterByName |
| condition    | ENUM(LIKE, NOT_LIKE, EQUAL, NOT_EQUAL, INCLUDE, NOT_INCLUDE) | √   | 条件    |
| pipelineName | string                                                       | √   | 流水线名字 |

#### PipelineViewFilterByCreator
 *多态基类 <PipelineViewFilter> 的实现处, 其中当字段 @type = [filterByCreator] 时指定为该类实现*
 

| 参数名称      | 参数类型                                                         | 必须  | 参数说明    |
| --------- | ------------------------------------------------------------ | --- | ------- |
| @type     | string                                                       | 必须是 | 多态类实现   | filterByCreator |
| condition | ENUM(LIKE, NOT_LIKE, EQUAL, NOT_EQUAL, INCLUDE, NOT_INCLUDE) | √   | 条件      |
| userIds   | List<string>                                                 | √   | 用户id 列表 |

#### PipelineViewFilterByLabel
 *多态基类 <PipelineViewFilter> 的实现处, 其中当字段 @type = [filterByLabel] 时指定为该类实现*
 

| 参数名称      | 参数类型                                                         | 必须  | 参数说明   |
| --------- | ------------------------------------------------------------ | --- | ------ |
| @type     | string                                                       | 必须是 | 多态类实现  | filterByLabel |
| condition | ENUM(LIKE, NOT_LIKE, EQUAL, NOT_EQUAL, INCLUDE, NOT_INCLUDE) | √   | 条件     |
| groupId   | string                                                       | √   | 流水线id  |
| labelIds  | List<string>                                                 | √   | 标签id列表 |

#### PipelineViewFilterByPacRepo
 *多态基类 <PipelineViewFilter> 的实现处, 其中当字段 @type = [filterByPacRepo] 时指定为该类实现*
 

| 参数名称       | 参数类型                                                         | 必须  | 参数说明      |
| ---------- | ------------------------------------------------------------ | --- | --------- |
| @type      | string                                                       | 必须是 | 多态类实现     | filterByPacRepo |
| condition  | ENUM(LIKE, NOT_LIKE, EQUAL, NOT_EQUAL, INCLUDE, NOT_INCLUDE) | √   | 条件        |
| directory  | string                                                       |     | 文件夹名称     |
| repoHashId | string                                                       | √   | 代码库HashId |

#### ResultPipelineViewId
##### 数据返回包装模型

| 参数名称    | 参数类型                              | 必须  | 参数说明 |
| ------- | --------------------------------- | --- | ---- |
| data    | [PipelineViewId](#PipelineViewId) |     |      |
| message | string                            |     | 错误信息 |
| status  | integer                           | √   | 状态码  |

#### PipelineViewId
##### 数据

| 参数名称 | 参数类型   | 必须  | 参数说明 |
| ---- | ------ | --- | ---- |
| id   | string |     |      |

 
