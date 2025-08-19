### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/environment/update_agent_info
### 资源描述
#### 修改Agent信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称 | 参数类型                                | 必须   |
| ---- | ----------------------------------- | ---- |
| 修改数据 | [UpdateAgentInfo](#UpdateAgentInfo) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "agentHashId" : "",
  "displayName" : "",
  "dockerParallelTaskCount" : 0,
  "envs" : [ {
    "name" : "",
    "secure" : false,
    "value" : ""
  } ],
  "nodeHashId" : "",
  "parallelTaskCount" : 0
}
```

### default 返回样例

```Json
{
  "data" : false,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### UpdateAgentInfo
##### 

| 参数名称                    | 参数类型                    | 必须  | 参数说明                 |
| ----------------------- | ----------------------- | --- | -------------------- |
| agentHashId             | string                  | √   | Agent Hash ID        |
| displayName             | string                  | √   | 节点名称                 |
| dockerParallelTaskCount | integer                 | √   | docker构建机通道数量        |
| envs                    | List<[EnvVar](#EnvVar)> | √   | Agent配置的环境变量,修改会直接覆盖 |
| nodeHashId              | string                  | √   | Node Hash ID         |
| parallelTaskCount       | integer                 | √   | 最大构建并发数              |

#### EnvVar
##### 环境变量

| 参数名称   | 参数类型    | 必须  | 参数说明   |
| ------ | ------- | --- | ------ |
| name   | string  | √   | 变量名    |
| secure | boolean | √   | 是否安全变量 |
| value  | string  | √   | 变量值    |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
