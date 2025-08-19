### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/environment/batch_update_agent_env
### 资源描述
#### 批量修改Agent环境变量
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

| 参数名称 | 参数类型                                              | 必须   |
| ---- | ------------------------------------------------- | ---- |
| 修改数据 | [BatchUpdateAgentEnvVar](#BatchUpdateAgentEnvVar) | true |

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
  "agentHashIds" : [ "" ],
  "envVars" : [ {
    "name" : "",
    "secure" : false,
    "value" : ""
  } ],
  "nodeHashIds" : [ "" ],
  "type" : "enum"
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
#### BatchUpdateAgentEnvVar
##### 批量修改Agent环境变量数据

| 参数名称         | 参数类型                      | 必须  | 参数说明                                     |
| ------------ | ------------------------- | --- | ---------------------------------------- |
| agentHashIds | List<string>              |     | agent Hash ID列表,和 nodeHashId 选其一即可       |
| envVars      | List<[EnvVar](#EnvVar)>   | √   | 环境变量                                     |
| nodeHashIds  | List<string>              |     | Node Hash ID列表,和 agentHashId 选其一即可       |
| type         | ENUM(ADD, REMOVE, UPDATE) |     | 修改方式,支持3种输入(ADD,REMOVE,UPDATE),默认为UPDATE |

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

 
