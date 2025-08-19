### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/environment/third_part_agent_nodes
### 资源描述
#### 获取项目下第三方构建机列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | projectId  |

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

| HTTP代码  | 参数类型                                              | 说明               |
| ------- | ------------------------------------------------- | ---------------- |
| default | [ResultListNodeBaseInfo](#ResultListNodeBaseInfo) | default response |

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
    "agentStatus" : false,
    "bakOperator" : "",
    "bizId" : 0,
    "createdUser" : "",
    "displayName" : "",
    "envEnableNode" : false,
    "gateway" : "",
    "ip" : "",
    "lastModifyTime" : 0,
    "name" : "",
    "nodeHashId" : "",
    "nodeId" : "",
    "nodeName" : "",
    "nodeStatus" : "",
    "nodeType" : "",
    "operator" : "",
    "osName" : "",
    "size" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListNodeBaseInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                                | 必须  | 参数说明 |
| ------- | ----------------------------------- | --- | ---- |
| data    | List<[NodeBaseInfo](#NodeBaseInfo)> |     | 数据   |
| message | string                              |     | 错误信息 |
| status  | integer                             | √   | 状态码  |

#### NodeBaseInfo
##### NodeBaseInfo-节点信息(权限)

| 参数名称           | 参数类型    | 必须  | 参数说明               |
| -------------- | ------- | --- | ------------------ |
| agentStatus    | boolean |     | agent状态            |
| bakOperator    | string  |     | 备份责任人              |
| bizId          | integer |     | 所属业务, 默认-1表示没有绑定业务 |
| createdUser    | string  | √   | 创建人                |
| displayName    | string  |     | 显示名称               |
| envEnableNode  | boolean |     | 当前环境是否启用这个 node    |
| gateway        | string  |     | 网关地域               |
| ip             | string  | √   | IP                 |
| lastModifyTime | integer |     | 最后更新时间             |
| name           | string  | √   | 节点名称               |
| nodeHashId     | string  | √   | 环境 HashId          |
| nodeId         | string  | √   | 节点 Id              |
| nodeName       | string  |     | 主机名                |
| nodeStatus     | string  | √   | 节点状态               |
| nodeType       | string  | √   | 节点类型               |
| operator       | string  |     | 责任人                |
| osName         | string  |     | 操作系统               |
| size           | string  |     | 机型                 |

 
