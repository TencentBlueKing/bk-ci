### 请求方法/请求路径
#### GET /{apigwType}/v4/auth/project/{projectId}/get_project_group_and_users
### 资源描述
#### 获取项目组成员
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

| HTTP代码  | 参数类型                                                                  | 说明               |
| ------- | --------------------------------------------------------------------- | ---------------- |
| default | [ResultListBkAuthGroupAndUserList](#ResultListBkAuthGroupAndUserList) | default response |

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
    "deptInfoList" : [ {
      "expired_at" : 0,
      "id" : "",
      "name" : "",
      "type" : ""
    } ],
    "dept_info_list" : [ {
      "expired_at" : 0,
      "id" : "",
      "name" : "",
      "type" : ""
    } ],
    "displayName" : "",
    "display_name" : "",
    "roleId" : 0,
    "roleName" : "",
    "role_id" : 0,
    "role_name" : "",
    "type" : "",
    "userIdList" : [ "" ],
    "user_id_list" : [ "" ]
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListBkAuthGroupAndUserList
##### 数据返回包装模型

| 参数名称    | 参数类型                                                    | 必须  | 参数说明 |
| ------- | ------------------------------------------------------- | --- | ---- |
| data    | List<[BkAuthGroupAndUserList](#BkAuthGroupAndUserList)> |     | 数据   |
| message | string                                                  |     | 错误信息 |
| status  | integer                                                 | √   | 状态码  |

#### BkAuthGroupAndUserList
##### 数据

| 参数名称           | 参数类型                                              | 必须  | 参数说明 |
| -------------- | ------------------------------------------------- | --- | ---- |
| deptInfoList   | List<[RoleGroupMemberInfo](#RoleGroupMemberInfo)> |     |      |
| dept_info_list | List<[RoleGroupMemberInfo](#RoleGroupMemberInfo)> |     |      |
| displayName    | string                                            |     |      |
| display_name   | string                                            |     |      |
| roleId         | integer                                           |     |      |
| roleName       | string                                            |     |      |
| role_id        | integer                                           |     |      |
| role_name      | string                                            |     |      |
| type           | string                                            |     |      |
| userIdList     | List<string>                                      |     |      |
| user_id_list   | List<string>                                      |     |      |

#### RoleGroupMemberInfo
##### 

| 参数名称       | 参数类型    | 必须  | 参数说明 |
| ---------- | ------- | --- | ---- |
| expired_at | integer |     |      |
| id         | string  |     |      |
| name       | string  |     |      |
| type       | string  |     |      |

 
