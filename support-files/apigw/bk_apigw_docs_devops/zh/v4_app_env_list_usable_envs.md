### 请求方法/请求路径
#### GET /{apigwType}/v4/environment/projects/{projectId}/usable_server_envs
### 资源描述
#### 获取用户有权限使用的CMDB环境列表
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

| HTTP代码  | 参数类型                                                        | 说明               |
| ------- | ----------------------------------------------------------- | ---------------- |
| default | [ResultListEnvWithPermission](#ResultListEnvWithPermission) | default response |

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
    "canDelete" : false,
    "canEdit" : false,
    "canUse" : false,
    "createdTime" : 0,
    "createdUser" : "",
    "desc" : "",
    "envHashId" : "",
    "envType" : "",
    "envVars" : [ {
      "name" : "",
      "secure" : false,
      "value" : ""
    } ],
    "name" : "",
    "nodeCount" : 0,
    "projectName" : "",
    "updatedTime" : 0,
    "updatedUser" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListEnvWithPermission
##### 数据返回包装模型

| 参数名称    | 参数类型                                          | 必须  | 参数说明 |
| ------- | --------------------------------------------- | --- | ---- |
| data    | List<[EnvWithPermission](#EnvWithPermission)> |     | 数据   |
| message | string                                        |     | 错误信息 |
| status  | integer                                       | √   | 状态码  |

#### EnvWithPermission
##### 环境信息(权限)

| 参数名称        | 参数类型                    | 必须  | 参数说明                                   |
| ----------- | ----------------------- | --- | -------------------------------------- |
| canDelete   | boolean                 |     | 是否可以删除                                 |
| canEdit     | boolean                 |     | 是否可以编辑                                 |
| canUse      | boolean                 |     | 是否可以使用                                 |
| createdTime | integer                 | √   | 创建时间                                   |
| createdUser | string                  | √   | 创建人                                    |
| desc        | string                  | √   | 环境描述                                   |
| envHashId   | string                  | √   | 环境 HashId                              |
| envType     | string                  | √   | 环境类型（开发环境{DEV}|测试环境{TEST}|构建环境{BUILD}） |
| envVars     | List<[EnvVar](#EnvVar)> |     | 环境变量                                   |
| name        | string                  | √   | 环境名称                                   |
| nodeCount   | integer                 |     | 节点数量                                   |
| projectName | string                  |     | 项目名称                                   |
| updatedTime | integer                 | √   | 更新时间                                   |
| updatedUser | string                  | √   | 更新人                                    |

#### EnvVar
##### 环境变量

| 参数名称   | 参数类型    | 必须  | 参数说明   |
| ------ | ------- | --- | ------ |
| name   | string  | √   | 变量名    |
| secure | boolean | √   | 是否安全变量 |
| value  | string  | √   | 变量值    |

 
