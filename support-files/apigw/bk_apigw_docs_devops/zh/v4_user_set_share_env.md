### 请求方法/请求路径
#### POST /{apigwType}/v4/environment/projects/{projectId}/share_envs
### 资源描述
#### 设置环境共享
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称      | 参数类型   | 必须  | 参数说明      |
| --------- | ------ | --- | --------- |
| envHashId | String | √   | 环境 hashId |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称    | 参数类型                                            | 必须   |
| ------- | ----------------------------------------------- | ---- |
| 共享的项目列表 | [SharedProjectInfoWrap](#SharedProjectInfoWrap) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?envHashId={envHashId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "sharedProjects" : [ {
    "finalProjectId" : "",
    "gitProjectId" : "",
    "name" : "",
    "projectId" : "",
    "type" : "enum"
  } ]
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
#### SharedProjectInfoWrap
##### 共享的项目列表

| 参数名称           | 参数类型                                                | 必须  | 参数说明 |
| -------------- | --------------------------------------------------- | --- | ---- |
| sharedProjects | List<[AddSharedProjectInfo](#AddSharedProjectInfo)> | √   |      |

#### AddSharedProjectInfo
##### VM虚拟机配额

| 参数名称           | 参数类型                 | 必须  | 参数说明                             |
| -------------- | -------------------- | --- | -------------------------------- |
| finalProjectId | string               |     |                                  |
| gitProjectId   | string               |     | 工蜂项目ID                           |
| name           | string               | √   | 项目名称，工蜂项目则为groupName/projectName |
| projectId      | string               |     | 项目ID                             |
| type           | ENUM(PROJECT, GROUP) | √   | 类型，预留                            |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
