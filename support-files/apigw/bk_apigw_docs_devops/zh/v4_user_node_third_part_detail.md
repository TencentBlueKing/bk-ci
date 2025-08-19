### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/environment/third_part_agent_node_detail
### 资源描述
#### 获取指定第三方构建机详情信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | projectId  |

#### Query参数

| 参数名称        | 参数类型   | 必须  | 参数说明                                                      |
| ----------- | ------ | --- | --------------------------------------------------------- |
| agentHashId | String |     | 节点 agentId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可) |
| nodeHashId  | String |     | 节点 hashId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)  |
| nodeName    | String |     | 节点 别名 (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)      |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                        | 说明               |
| ------- | ----------------------------------------------------------- | ---------------- |
| default | [ResultThirdPartyAgentDetail](#ResultThirdPartyAgentDetail) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?agentHashId={agentHashId}&nodeHashId={nodeHashId}&nodeName={nodeName}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "agentId" : "",
    "agentInstallPath" : "",
    "agentScript" : "",
    "agentUrl" : "",
    "agentVersion" : "",
    "canEdit" : false,
    "createdTime" : "",
    "createdUser" : "",
    "currentAgentVersion" : "",
    "currentWorkerVersion" : "",
    "diskTotal" : "",
    "displayName" : "",
    "dockerParallelTaskCount" : "",
    "exitErrorMsg" : "",
    "heartbeatInfo" : {
      "agentId" : 0,
      "agentInstallPath" : "",
      "agentIp" : "",
      "busyTaskSize" : 0,
      "dockerBusyTaskSize" : 0,
      "dockerParallelTaskCount" : 0,
      "dockerTaskList" : [ {
        "buildId" : "",
        "projectId" : "",
        "vmSeqId" : ""
      } ],
      "errorExitData" : {
        "errorEnum" : "",
        "message" : ""
      },
      "heartbeatTime" : 0,
      "hostName" : "",
      "masterVersion" : "",
      "parallelTaskCount" : 0,
      "projectId" : "",
      "props" : {
        "arch" : "",
        "dockerInitFileInfo" : {
          "fileMd5" : "",
          "needUpgrade" : false
        },
        "dockerInitFileMd5" : {
          "fileMd5" : "",
          "needUpgrade" : false
        },
        "jdkVersion" : [ "" ],
        "osVersion" : ""
      },
      "slaveVersion" : "",
      "startedUser" : "",
      "taskList" : [ {
        "buildId" : "",
        "projectId" : "",
        "vmSeqId" : "",
        "workspace" : ""
      } ]
    },
    "hostname" : "",
    "ip" : "",
    "lastHeartbeatTime" : "",
    "maxParallelTaskCount" : "",
    "memTotal" : "",
    "ncpus" : "",
    "nodeId" : "",
    "os" : "",
    "osName" : "",
    "parallelTaskCount" : "",
    "projectId" : "",
    "slaveVersion" : "",
    "startedUser" : "",
    "status" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultThirdPartyAgentDetail
##### 数据返回包装模型

| 参数名称    | 参数类型                                            | 必须  | 参数说明 |
| ------- | ----------------------------------------------- | --- | ---- |
| data    | [ThirdPartyAgentDetail](#ThirdPartyAgentDetail) |     |      |
| message | string                                          |     | 错误信息 |
| status  | integer                                         | √   | 状态码  |

#### ThirdPartyAgentDetail
##### 第三方构建集详情

| 参数名称                    | 参数类型                                  | 必须  | 参数说明                       |
| ----------------------- | ------------------------------------- | --- | -------------------------- |
| agentId                 | string                                | √   | Agent Hash ID              |
| agentInstallPath        | string                                | √   | agent安装路径                  |
| agentScript             | string                                | √   | agent安装脚本                  |
| agentUrl                | string                                | √   | agent链接                    |
| agentVersion            | string                                | √   | Agent版本                    |
| canEdit                 | boolean                               |     | 是否可以编辑                     |
| createdTime             | string                                | √   | 导入时间                       |
| createdUser             | string                                | √   | 导入人                        |
| currentAgentVersion     | string                                |     | 当前Agent版本                  |
| currentWorkerVersion    | string                                |     | 当前Worker版本                 |
| diskTotal               | string                                | √   | 硬盘空间（最大盘）                  |
| displayName             | string                                | √   | 节点名称                       |
| dockerParallelTaskCount | string                                | √   | docker构建机通道数量              |
| exitErrorMsg            | string                                |     | 错误退出信息                     |
| heartbeatInfo           | [NewHeartbeatInfo](#NewHeartbeatInfo) |     |                            |
| hostname                | string                                | √   | 主机名                        |
| ip                      | string                                | √   | IP地址                       |
| lastHeartbeatTime       | string                                | √   | 最新心跳时间                     |
| maxParallelTaskCount    | string                                | √   | 已废弃，使用 parallelTaskCount   |
| memTotal                | string                                | √   | 内存                         |
| ncpus                   | string                                | √   | CPU 核数                     |
| nodeId                  | string                                | √   | Node Hash ID               |
| os                      | string                                | √   | 操作系统 | LINUX MACOS WINDOWS |
| osName                  | string                                | √   | 操作系统                       |
| parallelTaskCount       | string                                | √   | 最大构建并发数                    |
| projectId               | string                                | √   | 项目ID                       |
| slaveVersion            | string                                | √   | Worker版本                   |
| startedUser             | string                                | √   | 启动用户                       |
| status                  | string                                | √   | 状态                         |

#### NewHeartbeatInfo
##### 心跳信息模型

| 参数名称                    | 参数类型                                                          | 必须  | 参数说明            |
| ----------------------- | ------------------------------------------------------------- | --- | --------------- |
| agentId                 | integer                                                       |     | 构建机id           |
| agentInstallPath        | string                                                        | √   | 构建机安装路径         |
| agentIp                 | string                                                        | √   | 构建机模型           |
| busyTaskSize            | integer                                                       | √   | 忙碌运行中任务数量       |
| dockerBusyTaskSize      | integer                                                       | √   | 忙碌运行docker中任务数量 |
| dockerParallelTaskCount | integer                                                       |     | docker并行任务计数    |
| dockerTaskList          | List<[ThirdPartyDockerBuildInfo](#ThirdPartyDockerBuildInfo)> |     | docker构建信息列表    |
| errorExitData           | [AgentErrorExitData](#AgentErrorExitData)                     |     |                 |
| heartbeatTime           | integer                                                       |     | 心跳时间戳           |
| hostName                | string                                                        | √   | 主机名             |
| masterVersion           | string                                                        | √   | 主版本             |
| parallelTaskCount       | integer                                                       | √   | 并行任务计数          |
| projectId               | string                                                        |     | 项目id            |
| props                   | [AgentPropsInfo](#AgentPropsInfo)                             |     |                 |
| slaveVersion            | string                                                        | √   | 从属版本            |
| startedUser             | string                                                        | √   | 启动者             |
| taskList                | List<[ThirdPartyBuildInfo](#ThirdPartyBuildInfo)>             |     | 第三方构建信息列表       |

#### ThirdPartyDockerBuildInfo
##### 第三方构建Docker信息

| 参数名称      | 参数类型   | 必须  | 参数说明    |
| --------- | ------ | --- | ------- |
| buildId   | string | √   | 构建id    |
| projectId | string | √   | 项目id    |
| vmSeqId   | string | √   | 构建机编排序号 |

#### AgentErrorExitData
##### Agent退出的错误信息

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| errorEnum | string |     |      |
| message   | string |     |      |

#### AgentPropsInfo
##### Agent属性信息

| 参数名称               | 参数类型                                      | 必须  | 参数说明           |
| ------------------ | ----------------------------------------- | --- | -------------- |
| arch               | string                                    | √   | agent运行系统的架构信息 |
| dockerInitFileInfo | [DockerInitFileInfo](#DockerInitFileInfo) |     |                |
| dockerInitFileMd5  | [DockerInitFileInfo](#DockerInitFileInfo) |     |                |
| jdkVersion         | List<string>                              |     | jdk版本信息        |
| osVersion          | string                                    |     |                |

#### DockerInitFileInfo
##### docker init 文件升级信息

| 参数名称        | 参数类型    | 必须  | 参数说明                     |
| ----------- | ------- | --- | ------------------------ |
| fileMd5     | string  | √   | 文件md5值                   |
| needUpgrade | boolean | √   | 目前只支持linux机器，所以其他系统不需要检查 |

#### ThirdPartyBuildInfo
##### 第三方构建信息

| 参数名称      | 参数类型   | 必须  | 参数说明    |
| --------- | ------ | --- | ------- |
| buildId   | string | √   | 构建id    |
| projectId | string | √   | 项目id    |
| vmSeqId   | string | √   | 构建机编排序号 |
| workspace | string | √   | 工作空间    |

 
