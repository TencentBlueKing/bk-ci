## bkrepo 角色相关接口

### 创建角色

- API: POST /auth/api/role/create
- API 名称: create_role
- 功能说明：
	- 中文：创建角色
	- English：create role

- input body:


``` json
{
    "name":"运维",
    "projectId":"ops",
    "rid":"operation",
    "type":"PROJECT",
    "admin":false
}
```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|name|string|是|无|角色名|the role name|
|projectId|string|是|无|项目id|the project id|
|rid|string|是|无|角色id|the role id|
|type|ENUM|是|无|角色类型[REPO,PROJECT]|the type of role[REPO,PROJECT]|
|admin|bool|否|false|是否管理员|is admin|



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


### 删除角色

- API: DELETE /auth/api/role/delete/{id}

- API 名称: delete_role
- 功能说明：
	- 中文：删除角色
	- English：delete role

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|id|string|是|无|角色主键id|the role key id|

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

### 角色详情

- API: GET /auth/api/role/detail/{id}

- API 名称: role_detail
- 功能说明：
	- 中文：角色详情
	- English ：role detail

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|id|string|是|无|角色主键id|the role key id|

- output:

```
{
    "code":0,
    "message":null,
    "data":{
        "admin":false,
        "id":"aaaaaaaa",
        "name":"运维",
        "projectId":"ops",
        "rid":"op",
        "type":"PROJECT"
    },
    "traceId":""
}


```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 角色列表

- API: GET /auth/api/role/list?projectId=ops&repoName=dockerlocal

- API 名称: list_role
- 功能说明：
	- 中文：角色列表
	- English：list role

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|否|无|项目id|the project id|
|repoName|string|否|无|仓库名称|the name of repository|

- output:

```
{
    "code":0,
    "message":null,
    "data":[
        {
            "admin":false,
            "id":"aaaaaaaa",
            "name":"运维",
            "projectId":"ops",
            "roleId":"op",
            "type":"PROJECT"
        }
    ],
    "traceId":""
}


```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object array | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|


### 查询角色下的用户

- API: GET /auth/api/role/users/{id}

- API 名称: role_user_list
- 功能说明：
	- 中文：角色绑定用户列表
	- English：role user list

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|id|string|是|无|角色主键id|the role primary key|

- output:

```
{
    "code":0,
    "message":null,
    "data":[
        {
            "id":"owen",
            "name":"owen"
        }
    ],
    "traceId":""
}


```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object array | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|id | string | 用户id |the user id|
|name|string|用户名|the user name|




