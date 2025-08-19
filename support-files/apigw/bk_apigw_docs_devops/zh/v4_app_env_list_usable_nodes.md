### 请求方法/请求路径
#### GET /{apigwType}/v4/environment/projects/{projectId}/usable_server_nodes
### 资源描述
#### 获取用户有权限使用的CMDB服务器列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
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

| HTTP代码  | 参数类型                                                          | 说明               |
| ------- | ------------------------------------------------------------- | ---------------- |
| default | [ResultListNodeWithPermission](#ResultListNodeWithPermission) | default response |

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
    "agentHashId" : "",
    "agentStatus" : false,
    "agentVersion" : "",
    "bakOperator" : "",
    "bizId" : 0,
    "bkHostId" : 0,
    "canDelete" : false,
    "canEdit" : false,
    "canUse" : false,
    "canView" : false,
    "cloudAreaId" : 0,
    "createTime" : "",
    "createdUser" : "",
    "displayName" : "",
    "envNames" : [ "" ],
    "gateway" : "",
    "ip" : "",
    "lastBuildTime" : "",
    "lastModifyTime" : "",
    "lastModifyUser" : "",
    "latestBuildDetail" : {
      "agentId" : "",
      "agentTask" : {
        "status" : ""
      },
      "buildId" : "",
      "buildNumber" : 0,
      "createdTime" : 0,
      "nodeId" : "",
      "pipelineId" : "",
      "pipelineName" : "",
      "projectId" : "",
      "status" : "",
      "taskName" : "",
      "updatedTime" : 0,
      "vmSetId" : "",
      "workspace" : ""
    },
    "name" : "",
    "nodeHashId" : "",
    "nodeId" : "",
    "nodeStatus" : "",
    "nodeType" : "",
    "operator" : "",
    "osName" : "",
    "osType" : "",
    "pipelineRefCount" : 0,
    "serverId" : 0,
    "size" : "",
    "tags" : [ {
      "canUpdate" : "enum",
      "tagAllowMulValue" : false,
      "tagKeyId" : 0,
      "tagKeyName" : "",
      "tagValues" : [ {
        "canUpdate" : "enum",
        "nodeCount" : 0,
        "tagValueId" : 0,
        "tagValueName" : ""
      } ]
    } ],
    "taskId" : 0
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListNodeWithPermission
##### 数据返回包装模型

| 参数名称    | 参数类型                                            | 必须  | 参数说明 |
| ------- | ----------------------------------------------- | --- | ---- |
| data    | List<[NodeWithPermission](#NodeWithPermission)> |     | 数据   |
| message | string                                          |     | 错误信息 |
| status  | integer                                         | √   | 状态码  |

#### NodeWithPermission
##### NodeWithPermission-节点信息(权限)

| 参数名称              | 参数类型                                  | 必须  | 参数说明               |
| ----------------- | ------------------------------------- | --- | ------------------ |
| agentHashId       | string                                |     | agent hash id      |
| agentStatus       | boolean                               | √   | agent状态            |
| agentVersion      | string                                |     | agent版本            |
| bakOperator       | string                                |     | 备份责任人              |
| bizId             | integer                               |     | 所属业务, 默认-1表示没有绑定业务 |
| bkHostId          | integer                               |     | hostID             |
| canDelete         | boolean                               |     | 是否可以删除             |
| canEdit           | boolean                               |     | 是否可以编辑             |
| canUse            | boolean                               |     | 是否可以使用             |
| canView           | boolean                               |     | 是否可以查看             |
| cloudAreaId       | integer                               |     | 云区域ID              |
| createTime        | string                                |     | 创建/导入时间            |
| createdUser       | string                                | √   | 创建人                |
| displayName       | string                                |     | 显示名称               |
| envNames          | List<string>                          |     | 该节点所属环境名           |
| gateway           | string                                |     | 网关地域               |
| ip                | string                                | √   | IP                 |
| lastBuildTime     | string                                |     | 流水线Job引用数          |
| lastModifyTime    | string                                |     | 最后修改时间             |
| lastModifyUser    | string                                |     | 最后修改人              |
| latestBuildDetail | [AgentBuildDetail](#AgentBuildDetail) |     |                    |
| name              | string                                | √   | 节点名称               |
| nodeHashId        | string                                | √   | 环境 HashId          |
| nodeId            | string                                | √   | 节点 Id              |
| nodeStatus        | string                                | √   | 节点状态               |
| nodeType          | string                                | √   | 节点类型               |
| operator          | string                                |     | 责任人                |
| osName            | string                                |     | 操作系统               |
| osType            | string                                |     | 操作系统类型             |
| pipelineRefCount  | integer                               |     | 流水线Job引用数          |
| serverId          | integer                               |     | 主机serverId         |
| size              | string                                |     | 机型                 |
| tags              | List<[NodeTag](#NodeTag)>             |     | 节点标签信息             |
| taskId            | integer                               |     | job任务ID            |

#### AgentBuildDetail
##### 第三方构建机构建任务详情

| 参数名称         | 参数类型                    | 必须  | 参数说明          |
| ------------ | ----------------------- | --- | ------------- |
| agentId      | string                  | √   | Agent Hash ID |
| agentTask    | [AgentTask](#AgentTask) |     |               |
| buildId      | string                  | √   | 构建ID          |
| buildNumber  | integer                 | √   | 构建号           |
| createdTime  | integer                 | √   | 创建时间          |
| nodeId       | string                  | √   | 节点 Hash ID    |
| pipelineId   | string                  | √   | 流水线ID         |
| pipelineName | string                  | √   | 流水线名称         |
| projectId    | string                  | √   | 项目ID          |
| status       | string                  | √   | 项目ID          |
| taskName     | string                  | √   | 构建任务名称        |
| updatedTime  | integer                 | √   | 更新时间          |
| vmSetId      | string                  | √   | VM_SET_ID     |
| workspace    | string                  | √   | 工作空间          |

#### AgentTask
##### Agent任务

| 参数名称   | 参数类型   | 必须  | 参数说明   |
| ------ | ------ | --- | ------ |
| status | string | √   | Task状态 |

#### NodeTag
##### 节点标签

| 参数名称             | 参数类型                                | 必须  | 参数说明                |
| ---------------- | ----------------------------------- | --- | ------------------- |
| canUpdate        | ENUM(INTERNAL, TRUE, FALSE)         |     | 标签是否可以修改区分          |
| tagAllowMulValue | boolean                             | √   | 当前节点标签是否支持一个节点多个标签值 |
| tagKeyId         | integer                             | √   | 节点标签名ID             |
| tagKeyName       | string                              | √   | 节点标签名               |
| tagValues        | List<[NodeTagValue](#NodeTagValue)> | √   | 节点标签值               |

#### NodeTagValue
##### 节点标签值

| 参数名称         | 参数类型                        | 必须  | 参数说明       |
| ------------ | --------------------------- | --- | ---------- |
| canUpdate    | ENUM(INTERNAL, TRUE, FALSE) |     | 标签是否可以修改区分 |
| nodeCount    | integer                     |     | 标签包含的节点数量  |
| tagValueId   | integer                     | √   | 节点值ID      |
| tagValueName | string                      | √   | 节点值名       |

 
