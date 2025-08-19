### 请求方法/请求路径
#### POST /{apigwType}/v4/environment/projects/{projectId}/fetch_nodes
### 资源描述
#### 获取项目节点列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   | 项目ID |

#### Query参数

| 参数名称                  | 参数类型                                                                                                                                                                                                                 | 必须  | 参数说明                              |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | --------------------------------- |
| agentVersion          | String                                                                                                                                                                                                               |     | Agent 版本                          |
| collation             | String                                                                                                                                                                                                               |     | 正序ASC/倒序DESC (默认倒序)               |
| createdUser           | String                                                                                                                                                                                                               |     | 创建人                               |
| displayName           | String                                                                                                                                                                                                               |     | 别名                                |
| keywords              | String                                                                                                                                                                                                               |     | 关键字                               |
| lastModifiedUser      | String                                                                                                                                                                                                               |     | 最后修改人                             |
| latestBuildPipelineId | String                                                                                                                                                                                                               |     | 最近执行流水线                           |
| latestBuildTimeEnd    | integer                                                                                                                                                                                                              |     | 最近构建执行时间 (结束)                     |
| latestBuildTimeStart  | integer                                                                                                                                                                                                              |     | 最近构建执行时间 (开始)                     |
| nodeIp                | String                                                                                                                                                                                                               |     | IP                                |
| nodeStatus            | ENUM(NORMAL, ABNORMAL, NOT_INSTALLED, DELETED, LOST, CREATING, RUNNING, STARTING, STOPPING, STOPPED, RESTARTING, DELETING, BUILDING_IMAGE, BUILD_IMAGE_SUCCESS, BUILD_IMAGE_FAILED, NOT_IN_CC, NOT_IN_CMDB, UNKNOWN) |     | Agent 状态                          |
| nodeType              | ENUM(CMDB, DEVCLOUD, THIRDPARTY, OTHER, UNKNOWN)                                                                                                                                                                     |     | 节点类型|用途 (构建: THIRDPARTY;部署: CMDB) |
| osName                | String                                                                                                                                                                                                               |     | 操作系统                              |
| page                  | integer                                                                                                                                                                                                              |     | 第几页                               |
| pageSize              | integer                                                                                                                                                                                                              |     | 每页多少条                             |
| sortType              | String                                                                                                                                                                                                               |     | 排序字段                              |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称 | 参数类型                          | 必须  |
| ---- | ----------------------------- | --- |
|      | [NodeFetchReq](#NodeFetchReq) |     |

#### 响应参数

| HTTP代码  | 参数类型                                                          | 说明               |
| ------- | ------------------------------------------------------------- | ---------------- |
| default | [ResultPageNodeWithPermission](#ResultPageNodeWithPermission) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?agentVersion={agentVersion}&collation={collation}&createdUser={createdUser}&displayName={displayName}&keywords={keywords}&lastModifiedUser={lastModifiedUser}&latestBuildPipelineId={latestBuildPipelineId}&latestBuildTimeEnd={latestBuildTimeEnd}&latestBuildTimeStart={latestBuildTimeStart}&nodeIp={nodeIp}&nodeStatus={nodeStatus}&nodeType={nodeType}&osName={osName}&page={page}&pageSize={pageSize}&sortType={sortType}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "tags" : [ {
    "tagKeyId" : 0,
    "tagValues" : [ 0 ]
  } ]
}
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ {
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
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### NodeFetchReq
##### 节点列表查询信息

| 参数名称 | 参数类型                                      | 必须  | 参数说明   |
| ---- | ----------------------------------------- | --- | ------ |
| tags | List<[NodeTagFetchReq](#NodeTagFetchReq)> |     | 查询标签列表 |

#### NodeTagFetchReq
##### 标签搜索参数

| 参数名称      | 参数类型          | 必须  | 参数说明    |
| --------- | ------------- | --- | ------- |
| tagKeyId  | integer       | √   | 节点标签名ID |
| tagValues | List<integer> |     | 节点标签值列表 |

#### ResultPageNodeWithPermission
##### 数据返回包装模型

| 参数名称    | 参数类型                                              | 必须  | 参数说明 |
| ------- | ------------------------------------------------- | --- | ---- |
| data    | [PageNodeWithPermission](#PageNodeWithPermission) |     |      |
| message | string                                            |     | 错误信息 |
| status  | integer                                           | √   | 状态码  |

#### PageNodeWithPermission
##### 分页数据包装模型

| 参数名称       | 参数类型                                            | 必须  | 参数说明  |
| ---------- | ----------------------------------------------- | --- | ----- |
| count      | integer                                         | √   | 总记录行数 |
| page       | integer                                         | √   | 第几页   |
| pageSize   | integer                                         | √   | 每页多少条 |
| records    | List<[NodeWithPermission](#NodeWithPermission)> | √   | 数据    |
| totalPages | integer                                         | √   | 总共多少页 |

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

 
