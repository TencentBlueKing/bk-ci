### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/pipelineView
### 资源描述
#### 获取视图(流水线组)
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   |      |

#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明                                    |
| --------- | ------- | --- | --------------------------------------- |
| isProject | boolean |     | 维度是否为项目,和viewName搭配使用                   |
| viewId    | String  |     | 用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入 |
| viewName  | String  |     | 用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                            | 说明               |
| ------- | ----------------------------------------------- | ---------------- |
| default | [ResultPipelineNewView](#ResultPipelineNewView) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?isProject={isProject}&viewId={viewId}&viewName={viewName}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "createTime" : 0,
    "creator" : "",
    "filters" : [ {
      "@type" : ""
    } ],
    "id" : "",
    "logic" : "enum",
    "name" : "",
    "pipelineIds" : [ "" ],
    "projectId" : "",
    "projected" : false,
    "updateTime" : 0,
    "viewType" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPipelineNewView
##### 数据返回包装模型

| 参数名称    | 参数类型                                | 必须  | 参数说明 |
| ------- | ----------------------------------- | --- | ---- |
| data    | [PipelineNewView](#PipelineNewView) |     |      |
| message | string                              |     | 错误信息 |
| status  | integer                             | √   | 状态码  |

#### PipelineNewView
##### 流水线视图

| 参数名称        | 参数类型                                            | 必须  | 参数说明       |
| ----------- | ----------------------------------------------- | --- | ---------- |
| createTime  | integer                                         | √   | 创建时间       |
| creator     | string                                          | √   | 创建者        |
| filters     | List<[PipelineViewFilter](#PipelineViewFilter)> | √   | 流水线视图过滤器列表 |
| id          | string                                          | √   | 流水线id      |
| logic       | ENUM(AND, OR)                                   | √   | 逻辑符        |
| name        | string                                          | √   | 名称         |
| pipelineIds | List<string>                                    | √   | 流水线ID列表    |
| projectId   | string                                          | √   | 项目id       |
| projected   | boolean                                         | √   | 是否项目       |
| updateTime  | integer                                         | √   | 更新时间       |
| viewType    | integer                                         | √   | 视图类型       |

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

 
