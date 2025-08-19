### 请求方法/请求路径
#### GET /{apigwType}/v4/environment/projects/{projectId}/pipeline_ref_list
### 资源描述
#### 获取第三方构建节点被流水线引用数据
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称          | 参数类型   | 必须  | 参数说明                             |
| ------------- | ------ | --- | -------------------------------- |
| nodeHashId    | String | √   | 节点 hashId                        |
| sortBy        | String | √   | 排序字段, pipelineName|lastBuildTime |
| sortDirection | String | √   | 排序方向, ASC|DESC                   |

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
| default | [ResultListAgentPipelineRef](#ResultListAgentPipelineRef) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?nodeHashId={nodeHashId}&sortBy={sortBy}&sortDirection={sortDirection}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "agentHashId" : "",
    "agentId" : 0,
    "jobId" : "",
    "jobName" : "",
    "lastBuildTime" : "",
    "nodeHashId" : "",
    "nodeId" : 0,
    "pipelineId" : "",
    "pipelineName" : "",
    "projectId" : "",
    "vmSeqId" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListAgentPipelineRef
##### 数据返回包装模型

| 参数名称    | 参数类型                                        | 必须  | 参数说明 |
| ------- | ------------------------------------------- | --- | ---- |
| data    | List<[AgentPipelineRef](#AgentPipelineRef)> |     | 数据   |
| message | string                                      |     | 错误信息 |
| status  | integer                                     | √   | 状态码  |

#### AgentPipelineRef
##### 第三方构建机流水线引用信息

| 参数名称          | 参数类型    | 必须  | 参数说明          |
| ------------- | ------- | --- | ------------- |
| agentHashId   | string  |     | Agent Hash ID |
| agentId       | integer |     | Agent ID      |
| jobId         | string  |     | Job ID        |
| jobName       | string  | √   | Job Name      |
| lastBuildTime | string  |     | 上次构建时间        |
| nodeHashId    | string  |     | Node Hash ID  |
| nodeId        | integer |     | Node ID       |
| pipelineId    | string  | √   | 流水线ID         |
| pipelineName  | string  | √   | 流水线名称         |
| projectId     | string  | √   | 项目ID          |
| vmSeqId       | string  |     | Vm Seq ID     |

 
