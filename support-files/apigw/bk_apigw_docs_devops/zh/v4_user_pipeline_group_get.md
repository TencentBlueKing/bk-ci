### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/pipelineGroups/group
### 资源描述
#### 获取所有分组信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数
###### 无此参数
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
| default | [ResultListPipelineGroup](#ResultListPipelineGroup) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "createTime" : 0,
    "createUser" : "",
    "id" : "",
    "labels" : [ {
      "createTime" : 0,
      "createUser" : "",
      "groupId" : "",
      "id" : "",
      "name" : "",
      "updateUser" : "",
      "uptimeTime" : 0
    } ],
    "name" : "",
    "projectId" : "",
    "updateTime" : 0,
    "updateUser" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListPipelineGroup
##### 数据返回包装模型

| 参数名称    | 参数类型                                  | 必须  | 参数说明 |
| ------- | ------------------------------------- | --- | ---- |
| data    | List<[PipelineGroup](#PipelineGroup)> |     | 数据   |
| message | string                                |     | 错误信息 |
| status  | integer                               | √   | 状态码  |

#### PipelineGroup
##### 流水线标签组模型

| 参数名称       | 参数类型                                  | 必须  | 参数说明  |
| ---------- | ------------------------------------- | --- | ----- |
| createTime | integer                               | √   | 创建时间  |
| createUser | string                                | √   | 创建者   |
| id         | string                                | √   | id    |
| labels     | List<[PipelineLabel](#PipelineLabel)> | √   | 流水线标签 |
| name       | string                                | √   | 名称    |
| projectId  | string                                | √   | 项目id  |
| updateTime | integer                               | √   | 更新时间  |
| updateUser | string                                | √   | 更新者   |

#### PipelineLabel
##### 流水线标签

| 参数名称       | 参数类型    | 必须  | 参数说明  |
| ---------- | ------- | --- | ----- |
| createTime | integer | √   | 创建时间  |
| createUser | string  | √   | 创建者   |
| groupId    | string  | √   | 流水线id |
| id         | string  | √   | 标签id  |
| name       | string  | √   | 标签名称  |
| updateUser | string  | √   | 更新者   |
| uptimeTime | integer | √   | 更新时间  |

 
