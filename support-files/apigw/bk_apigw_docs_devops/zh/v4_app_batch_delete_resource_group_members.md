### 请求方法/请求路径
#### DELETE /{apigwType}/v4/auth/project/{projectId}/batch_delete_resource_group_members
### 资源描述
#### 用户组批量删除成员
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
| 删除信息 | [ProjectDeleteUserInfo](#ProjectDeleteUserInfo) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X DELETE '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### DELETE 请求样例

```Json
{
  "deptIds" : [ "" ],
  "groupId" : 0,
  "operator" : "",
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
#### ProjectDeleteUserInfo
##### 

| 参数名称         | 参数类型         | 必须  | 参数说明     |
| ------------ | ------------ | --- | -------- |
| deptIds      | List<string> |     | 目标部门     |
| groupId      | integer      |     | 组ID      |
| operator     | string       |     | 操作人      |
| resourceCode | string       |     | 资源Id     |
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

 
