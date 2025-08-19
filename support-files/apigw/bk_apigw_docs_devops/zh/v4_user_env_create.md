### 请求方法/请求路径
#### POST /{apigwType}/v4/environment/projects/{projectId}/envs
### 资源描述
#### 创建环境
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

| 参数名称 | 参数类型                            | 必须   |
| ---- | ------------------------------- | ---- |
| 环境信息 | [EnvCreateInfo](#EnvCreateInfo) | true |

#### 响应参数

| HTTP代码  | 参数类型                                        | 说明               |
| ------- | ------------------------------------------- | ---------------- |
| default | [ResultEnvironmentId](#ResultEnvironmentId) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "desc" : "",
  "envType" : "enum",
  "envVars" : [ {
    "name" : "",
    "secure" : false,
    "value" : ""
  } ],
  "name" : "",
  "nodeHashIds" : [ "" ],
  "source" : "enum"
}
```

### default 返回样例

```Json
{
  "data" : {
    "hashId" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### EnvCreateInfo
##### 环境信息

| 参数名称        | 参数类型                         | 必须  | 参数说明                                   |
| ----------- | ---------------------------- | --- | -------------------------------------- |
| desc        | string                       | √   | 环境描述                                   |
| envType     | ENUM(DEV, TEST, PROD, BUILD) | √   | 环境类型（开发环境{DEV}|测试环境{TEST}|构建环境{BUILD}） |
| envVars     | List<[EnvVar](#EnvVar)>      |     | 环境变量                                   |
| name        | string                       | √   | 环境名称                                   |
| nodeHashIds | List<string>                 |     | 节点 HashId 列表                           |
| source      | ENUM(EXISTING, CREATE, CMDB) | √   | 节点来源（已有节点{EXISTING}|快速生成{CREATE}）      |

#### EnvVar
##### 环境变量

| 参数名称   | 参数类型    | 必须  | 参数说明   |
| ------ | ------- | --- | ------ |
| name   | string  | √   | 变量名    |
| secure | boolean | √   | 是否安全变量 |
| value  | string  | √   | 变量值    |

#### ResultEnvironmentId
##### 数据返回包装模型

| 参数名称    | 参数类型                            | 必须  | 参数说明 |
| ------- | ------------------------------- | --- | ---- |
| data    | [EnvironmentId](#EnvironmentId) |     |      |
| message | string                          |     | 错误信息 |
| status  | integer                         | √   | 状态码  |

#### EnvironmentId
##### 环境-ID

| 参数名称   | 参数类型   | 必须  | 参数说明   |
| ------ | ------ | --- | ------ |
| hashId | string | √   | 环境哈希ID |

 
