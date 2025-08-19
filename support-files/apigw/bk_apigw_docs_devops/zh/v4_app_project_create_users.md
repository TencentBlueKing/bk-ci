### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/project/{projectId}/project_user
### 资源描述
#### 添加指定用户到指定项目用户组
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

| 参数名称 | 参数类型                                            | 必须   |
| ---- | ----------------------------------------------- | ---- |
| 添加信息 | [ProjectCreateUserInfo](#ProjectCreateUserInfo) | true |

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
  "createUserId" : "",
  "deptIds" : [ "" ],
  "expiredTime" : 0,
  "groupId" : 0,
  "resourceCode" : "",
  "resourceType" : "",
  "roleId" : 0,
  "roleName" : "",
  "userIds" : [ "" ]
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
#### ProjectCreateUserInfo
##### 

| 参数名称         | 参数类型         | 必须  | 参数说明     |
| ------------ | ------------ | --- | -------- |
| createUserId | string       |     | 操作人      |
| deptIds      | List<string> |     | 目标部门     |
| expiredTime  | integer      |     | 过期天数     |
| groupId      | integer      |     | 组ID      |
| resourceCode | string       |     | 资源ID     |
| resourceType | string       |     | 资源类型     |
| roleId       | integer      |     | 角色Id     |
| roleName     | string       |     | 待分配的角色名称 |
| userIds      | List<string> |     | 目标用户     |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
