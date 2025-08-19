### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/pipelineView/list
### 资源描述
#### 获取视图(流水线组)列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   | 项目ID |

#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明                           |
| --------- | ------- | --- | ------------------------------ |
| projected | boolean |     | 是否为项目流水线组 , 为空时不区分             |
| viewType  | integer |     | 流水线组类型 , 1--动态, 2--静态 , 为空时不区分 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                                  | 说明               |
| ------- | --------------------------------------------------------------------- | ---------------- |
| default | [ResultListPipelineNewViewSummary](#ResultListPipelineNewViewSummary) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?projected={projected}&viewType={viewType}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "createTime" : 0,
    "creator" : "",
    "id" : "",
    "name" : "",
    "pac" : false,
    "pipelineCount" : 0,
    "projectId" : "",
    "projected" : false,
    "top" : false,
    "updateTime" : 0,
    "viewType" : 0
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListPipelineNewViewSummary
##### 数据返回包装模型

| 参数名称    | 参数类型                                                    | 必须  | 参数说明 |
| ------- | ------------------------------------------------------- | --- | ---- |
| data    | List<[PipelineNewViewSummary](#PipelineNewViewSummary)> |     | 数据   |
| message | string                                                  |     | 错误信息 |
| status  | integer                                                 | √   | 状态码  |

#### PipelineNewViewSummary
##### 数据

| 参数名称          | 参数类型    | 必须  | 参数说明               |
| ------------- | ------- | --- | ------------------ |
| createTime    | integer |     | 创建时间               |
| creator       | string  |     | 创建者                |
| id            | string  |     | 视图id               |
| name          | string  |     | 视图名称               |
| pac           | boolean | √   | 是否是PAC流水线组         |
| pipelineCount | integer | √   | 流水线个数              |
| projectId     | string  |     | 项目id               |
| projected     | boolean |     | 是否项目               |
| top           | boolean |     | 是否置顶               |
| updateTime    | integer |     | 更新时间               |
| viewType      | integer | √   | 流水线组类型,1--动态,2--静态 |

 
