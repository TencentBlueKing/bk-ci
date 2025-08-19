### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/environment/third_part_agent_builds
### 资源描述
#### 获取第三方构建机任务
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称        | 参数类型    | 必须  | 参数说明                                                      |
| ----------- | ------- | --- | --------------------------------------------------------- |
| agentHashId | String  |     | 节点 agentId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可) |
| nodeHashId  | String  |     | 节点 hashId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)  |
| nodeName    | String  |     | 节点 别名 (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)      |
| page        | integer |     | 第几页                                                       |
| pageSize    | integer |     | 每页条数(默认20, 最大100)                                         |
| pipelineId  | String  |     | 筛选此pipelineId                                             |
| status      | String  |     | 筛选此状态，支持4种输入(QUEUE,RUNNING,DONE,FAILURE)                  |

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
| default | [ResultPageAgentBuildDetail](#ResultPageAgentBuildDetail) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?agentHashId={agentHashId}&nodeHashId={nodeHashId}&nodeName={nodeName}&page={page}&pageSize={pageSize}&pipelineId={pipelineId}&status={status}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ {
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
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageAgentBuildDetail
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | [PageAgentBuildDetail](#PageAgentBuildDetail) |     |      |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### PageAgentBuildDetail
##### 分页数据包装模型

| 参数名称       | 参数类型                                        | 必须  | 参数说明  |
| ---------- | ------------------------------------------- | --- | ----- |
| count      | integer                                     | √   | 总记录行数 |
| page       | integer                                     | √   | 第几页   |
| pageSize   | integer                                     | √   | 每页多少条 |
| records    | List<[AgentBuildDetail](#AgentBuildDetail)> | √   | 数据    |
| totalPages | integer                                     | √   | 总共多少页 |

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

 
