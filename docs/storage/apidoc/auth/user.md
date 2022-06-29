## bkrepo 用户相关接口

### 创建用户

- API: POST /auth/api/user/create
- API 名称: create_user
- 功能说明：
	- 中文：创建用户
	- English：create user

- input body:

``` json
{
    "userId":"public",
    "name":"public",
    "pwd":"blueking",
    "group":true,
    "asstUsers":["tt"]
}

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|name|string|是|无|用户名|the  name|
|pwd|string|是|无|用户密码|the user password|
|userId|string|是|无|用户id|the user id|
|asstUsers|string array|否|[]|关联用户|association user|
|group|boot |否|false|是否群组账号|is group user|
- output:

```
{
    "code":0,
    "message":null,
    "data":true,
    "traceId":""
}
```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|


### 创建用户到项目管理员

- API: POST /auth/api/user/create/project
- API 名称: create_user_to_project
- 功能说明：
	- 中文：创建用户
	- English：create user to project

- input body:

``` json
{
    "name":"string",
    "pwd":"string",
    "userId":"string",
    "asstUsers":[
        "owen",
        "necr"
    ],
    "group":true,
    "projectId":"test"
}
```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|name|string|是|无|用户名|the  name|
|pwd|string|是|无|用户密码|the user password|
|userId|string|是|无|用户id|the user id|
|admin|bool|否|false|是否管理员|is admin|
|asstUsers|string array|否|[]|关联用户|association user|
|group|boot |否|false|是否群组账号|is group user|
|projectId|string|是|无|关联到的项目|the association project|

- output:

```
{
"code": 0,
"message": null,
"data": true,
"traceId": ""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|


### 创建用户到仓库管理员

- API: POST /auth/api/user/create/repo
- API 名称: create_user_to_repo
- 功能说明：
	- 中文：创建用户为仓库管理员
	- English：create user to repo

- input body:

``` json
{
    "name":"string",
    "pwd":"string",
    "userId":"string",
    "asstUsers":[
        "owen",
        "necr"
    ],
    "group":true,
    "projectId":"test",
    "repoName":"generic"
}
```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|name|string|是|无|用户名|the  name|
|pwd|string|是|无|用户密码|the user password|
|userId|string|是|无|用户id|the user id|
|asstUsers|string array|否|[]|关联用户|association user|
|group|boot |否|false|是否群组账号|is group user|
|projectId|string|是|无|关联到的项目|the association project|
|repoName|string|是|无|关联到的仓库|the association repo|

- output:

```
{
"code": 0,
"message": null,
"data": true,
"traceId": ""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 用户列表

- API:GET /auth/api/user/list
- API 名称: list_user
- 功能说明：
	- 中文：用户列表
	- English：list user

- input body:

``` json

```

- input 字段说明

- output:

```
{
    "code":0,
    "data":[
        {
            "name":"owen",
            "userId":"owen"
        }
    ],
    "message":"",
    "traceId":""
}


```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object | user data info |the info of user|
|traceId|string|请求跟踪id|the trace id|


- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|userId | string | 用户id |the user id|
|name|string|用户名|the department name|


### 用户详情

- API:GET /auth/api/user/detail/{uid}
- API 名称: user_detail
- 功能说明：
	- 中文：用户详情
	- English：user detail

- input body:

``` json


```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|


- output:

```
{
    "code":0,
    "data":{
        "admin":true,
        "locked":true,
        "name":"string",
        "pwd":"string",
        "roles":[
            "abcdegfffff"
        ],
        "tokens":[
            {
                "createdAt":"2019-12-21T08:47:36.656Z",
                "expiredAt":"2019-12-21T08:47:36.656Z",
                "id":"ssss-deeedd"
            }
        ],
        "uid":"owen"
    },
    "message":"",
    "traceId":""
}


```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object | user data info |the info of user|
|traceId|string|请求跟踪id|the trace id|

- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|roles|string array|用户所属的角色列表 |the user role list|
|tokens|object array|用户创建的所有token |the tokens of user |

- tokens 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|id|string |token id |the token id|
|createdAt|date time| token创建时间|create time |
|expiredAt|date time|token失效 |expire time |

### 删除用户

- API:DELETE /auth/api/user/{uid}
- API 名称: delete_user
- 功能说明：
	- 中文：删除用户
	- English：delete user

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|

- output:

```
{
    "code":0,
    "message":null,
    "data":true,
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 更新用户信息

- API:PUT  /auth/api/user/{uid}
- API 名称: update_user
- 功能说明：
	- 中文：更新用户信息
	- English：update user

- input body:

``` json
{
    "admin":false,
    "name":"string",
    "pwd":"string",
    "asstUsers":["owen"]
}
```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|name|string|是|无|用户名|the  name|
|pwd|string|是|无|用户密码|the user password|
|uid|string|是|无|用户id|the user id|
|admin|bool|否|false|是否管理员|is admin|
|asstUsers|string array|否|false|关联用户|asst users|

- output:

```
{
    "code":0,
    "message":null,
    "data":true,
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|


### 删除用户所属角色

- API:DELETE  /auth/api/user/role/{uid}/{rid}
- API 名称: delete_user_role
- 功能说明：
	- 中文：删除用户所属角色
	- English：delete user role

- input body:

``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|
|rid|string|是|无|角色主键ID|the role key id|

- output:

```
{
    "code":0,
    "data":{
        "admin":true,
        "locked":true,
        "name":"string",
        "pwd":"string",
        "roles":[
            "string"
        ],
        "tokens":[
            {
                "createdAt":"2019-12-21T09:46:37.877Z",
                "expiredAt":"2019-12-21T09:46:37.877Z",
                "id":"string"
            }
        ],
        "uid":"string"
    },
    "message":"string",
    "traceId":"string"
}


```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 新增用户所属角色

- API:POST  /auth/api/user/role/{uid}/{rid}
- API 名称: add_user_role
- 功能说明：
	- 中文：新增用户所属角色
	- English：add user role

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|
|rid|string|是|无|角色主键ID|the role key id|

- output:

```
{
    "code":0,
    "data":{
        "admin":true,
        "locked":true,
        "name":"string",
        "pwd":"string",
        "roles":[
            "string"
        ],
        "tokens":[
            {
                "createdAt":"2019-12-21T09:46:37.877Z",
                "expiredAt":"2019-12-21T09:46:37.877Z",
                "id":"string"
            }
        ],
        "uid":"string"
    },
    "message":"string",
    "traceId":"string"
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|



### 批量新增用户所属角色

- API:PATCH /auth/api/user/role/add/{rid}
- API 名称: patch_create_user_role
- 功能说明：
	- 中文：批量新增用户所属角色
	- English：add user role patch

- input body:

``` json
[
    "owen",
    "tt"
]
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string array|是|无|用户id数组|the user id array|
|rid|string|是|无|角色主键ID|the role token|

- output:

```
{
    "code":0,
    "message":null,
    "data":true,
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 批量删除用户所属角色

- API:PATCH /auth/api/user/role/add/{rid}
- API 名称: patch_delete_user_role
- 功能说明：
	- 中文：批量删除用户所属角色
	- English：delete user role patch

- input body:

``` json
[
    "owen",
    "tt"
]
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string array|是|无|用户id数组|the user id array|
|rid|string|是|无|角色主键ID|the role token|

- output:

```
{
"code": 0,
"message": null,
"data": true,
"traceId": ""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 判断用户是否为项目管理员

- API:GET /auth/api/user/admin/{projectId}
- API 名称: is_project_admin
- 功能说明：
    - 中文：查询用户是否为项目管理员
    - English：query user is project admin or not

- input body:

``` json
```

- input 字段说明

| 字段        | 类型     | 是否必须 | 默认值 | 说明   | Description |
|-----------|--------|------|-----|------|-------------|
| projectId | string | 是    | 无   | 项目id | project id  |

- output:

```
{
"code": 0,
"message": null,
"data": true,
"traceId": ""
}

```

- output 字段说明

| 字段      | 类型             | 说明                        | Description                                    |
|---------|----------------|---------------------------|------------------------------------------------|
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure                      |
| message | result message | 错误消息                      | the failure message                            |
| data    | bool           | 是否为项目管理员                  | true if user is project admin, false otherwise |
| traceId | string         | 请求跟踪id                    | the trace id                                   |