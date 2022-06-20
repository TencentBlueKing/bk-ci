## bkrepo 权限相关接口

### 创建权限

- API: POST /auth/api/permission/create
- API 名称: create_permission
- 功能说明：
	- 中文：创建权限
	- English：create permission

- input body:

``` json
{
    "createBy":"owen",
    "excludePattern":[
        "/index"
    ],
    "includePattern":[
        "/path1"
    ],
    "permName":"perm1",
    "projectId":"ops",
    "repos":[
        "owen"
    ],
    "resourceType":"PROJECT",
    "roles":[
        "abcdef"
    ],
    "users":[
        "owen"
    ],
    "departments":[
        "1",
        "2"
    ],
    "actions":"MANAGE"
}

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|createBy|string|是|无|创建人|the man create it|
|excludePattern|string array|否|empty array|排除路径|the exclude path|
|includePattern|string array|否|empty array|包含路径|the include path|
|projectId|string|否|null|项目ID|the project id|
|repos|string array|否|empty array|关联仓库列表|the associate repo list|
|resourceType|ENUM|是|REPO|权限类型[REPO,PROJECT,SYSTEM,NODE]|permission type [REPO,PROJECT,SYSTEM,NODE]|
|users|string array|否|empty array|权限授权用户|the auth user|
|roles|string array|否|empty array|权限授权角色|the auth role|
|departments|string array|否|empty array|权限授权角色|the auth department|


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

### 删除权限

- API: DELETE /auth/api/permission/delete/{id}
- API 名称: delete_permission
- 功能说明：
	- 中文：删除权限
	- English：delete permission

- input body:

``` json


```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|id|string|是|无|权限主键ID|the permission key id|


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

### 校验权限

- API: POST /auth/api/permission/check
- API 名称: check_permission
- 功能说明：
	- 中文：校验权限
	- English：check permission

- input body:

``` json
{
    "action":"MANAGE",
    "path":"/index",
    "projectId":"ops",
    "repoName":"docker-local",
    "resourceType":"PROJECT",
    "uid":"owen"
}


```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|action|ENUM|是|无|动作|the action|
|path|string|否|无|路径|the path|
|projectId|string|是|无|项目ID|the project id |
|repoName|string|否|无|仓库名|the name of repo |
|resourceType|ENUM|是|REPO|权限类型[REPO,PROJECT,SYSTEM,NODE]|permission type [REPO,PROJECT,SYSTEM,NODE]|
|uid|string|是|无|用户ID|the user id|

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

### 校验管理员

- API: GET /auth/api/permission/checkAdmin/{uid}
- API 名称: check_admin
- 功能说明：
	- 中文：校验管理员权限
	- English：check admin permission

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户ID|the user id|

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



### 仓库内置权限列表

- API: GET /auth/api/permission/list/inrepo?projectId=ops&repoName=repo
- API 名称: list_permission
- 功能说明：
	- 中文：权限列表
	- English：the permission list
- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目ID|the project id|
|repoName|string|是|无|仓库名称|the repo name|
|resourceType|ENUM|否|无|权限类型[REPO,PROJECT,SYSTEM,NODE]|permission type [REPO,PROJECT,SYSTEM,NODE]|

- output:
```
{
  "code": 0,
  "message": null,
  "data": [
    {
      "id": "5fbcc9d85fe04f126a508a3a",
      "resourceType": "REPO",
      "projectId": "ops",
      "permName": "repo_admin",
      "repos": [
        "generic"
      ],
      "includePattern": [],
      "excludePattern": [],
      "users": [],
      "roles": [],
      "departments": [],
      "actions": [],
      "createBy": "admin",
      "updatedBy": "admin",
      "createAt": "2020-11-24T16:52:40.575",
      "updateAt": "2020-11-24T16:52:40.575"
    },
    {
      "id": "5fbcc9d85fe04f126a508a3b",
      "resourceType": "REPO",
      "projectId": "ops",
      "permName": "repo_user",
      "repos": [
        "generic"
      ],
      "includePattern": [],
      "excludePattern": [],
      "users": [],
      "roles": [],
      "departments": [],
      "actions": [],
      "createBy": "admin",
      "updatedBy": "admin",
      "createAt": "2020-11-24T16:52:40.678",
      "updateAt": "2020-11-24T16:52:40.678"
    },
    {
      "id": "5fbcc9d85fe04f126a508a3c",
      "resourceType": "REPO",
      "projectId": "ops",
      "permName": "repo_viewer",
      "repos": [
        "generic"
      ],
      "includePattern": [],
      "excludePattern": [],
      "users": [],
      "roles": [],
      "departments": [],
      "actions": [],
      "createBy": "admin",
      "updatedBy": "admin",
      "createAt": "2020-11-24T16:52:40.704",
      "updateAt": "2020-11-24T16:52:40.704"
    }
  ],
  "traceId": ""
}


```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object array | result data,具体字段见创建请求 |the data for response|
|traceId|string|请求跟踪id|the trace id|

- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|users|string array|用户id|the user id|
|roles|string array|用户组id|the role id|
|departments|string array|部门id|the department id|
|actions|string array|action id|the action id|

### 权限列表

- API: GET /auth/api/permission/list?projectId=ops&repoName=repo
- API 名称: list_permission
- 功能说明：
	- 中文：权限列表
	- English：the permission list
- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|否|无|项目ID|the project id|
|repoName|string|否|无|仓库名称|the repo name|

- output:
```
{
    "code":0,
    "data":[
        {
            "createAt":"2019-12-21T09:46:37.792Z",
            "createBy":"string",
            "excludePattern":[
                "/index"
            ],
            "id":"5ea4f6608c165f702f5bd41e",
            "includePattern":[
                "/path1"
            ],
            "permName":"perm1",
            "projectId":"ops",
            "repos":[
                "docker-local"
            ],
            "resourceType":"REPO",
            "roles":[
                "owen",
                "tt"
            ],
            "users":[
                "op",
                "dev"
            ],
            "departments":[
                "1",
                "2"
            ],
            "actions":[
                "MANAGE",
                "READ"
            ],
            "updateAt":"2019-12-21T09:46:37.792Z",
            "updatedBy":"string"
        }
    ],
    "message":"string",
    "traceId":"string"
}


```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object array | result data,具体字段见创建请求 |the data for response|
|traceId|string|请求跟踪id|the trace id|

- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|users|string array|用户id|the user id|
|roles|string array|用户组id|the role id|
|departments|string array|部门id|the department id|
|actions|string array|action id|the action id|

### 更新权限绑定仓库

- API: PUT /auth/api/permission/repo
- 功能说明：
	- 中文：更新权限绑定仓库
	- English：update permission repo
- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "repos":[
        "owen",
        "tt"
    ]
}
```
- input 字段说明


|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|repos|string array|是|[]|仓库名称列表|the repo name array|

- output:

```

```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object array | result data,具体字段见创建请求 |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 更新权限绑定用户

- API:PUT /auth/api/permission/user
- API 名称: update_permission_user
- 功能说明：
	- 中文：更新权限绑定用户
	- English：update permission user

- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "userId":[
        "owen",
        "tt"
    ]
}
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|userId|string array|是|[]|用户id列表|the user id array|

- output:

```
{
    "code":0,
    "data":true,
    "message":"",
    "traceId":""
}


```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | the request result |the request result|
|traceId|string|请求跟踪id|the trace id|

### 更新权限绑定角色

- API:PUT /auth/api/permission/role
- API 名称: update_permission_role
- 功能说明：
	- 中文：更新权限绑定角色
	- English：update permission role

- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "rId":[
        "ops",
        "dev"
    ]
}
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|rId|string array|是|[]|角色主键id列表|the role id primary key array|

- output:

```
{
    "code":0,
    "data":true,
    "message":"",
    "traceId":""
}

```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | the request result |the request result|
|traceId|string|请求跟踪id|the trace id|


### 更新权限绑定部门

- API:PUT /auth/api/permission/department
- API 名称: update_permission_department
- 功能说明：
	- 中文：更新角色绑定部门
	- English：update permission department 

- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "departmentId":[
        "ops",
        "dev"
    ]
}
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|departmentId|string array|是|[]|部门id列表|the department id array|

- output:

```
{
    "code":0,
    "data":true,
    "message":"",
    "traceId":""
}

```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | the request result |the request result|
|traceId|string|请求跟踪id|the trace id|

### 更新权限绑定动作

- API:PUT /auth/api/permission/action
- API 名称: update_permission_action
- 功能说明：
	- 中文：更新角色绑定动作
	- English：update permission department 

- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "actions":[
        "ops",
        "dev"
    ]
}
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|actions|string array|是|[]|动作列表|the action list|

- output:

```
{
    "code":0,
    "data":true,
    "message":"",
    "traceId":""
}

```
- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool | the request result |the request result|
|traceId|string|请求跟踪id|the trace id|


### 更新权限包含路径

- API: PUT /auth/api/permission/includePath
- API 名称: update_include_path
- 功能说明：
	- 中文：更新权限包含路径
	- English：update permission include path
- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "path":[
        "/path1",
        "/path2"
    ]
}
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|path|string array|是|[]|路径列表|the path list|

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


### 更新权限排除路径

- API: PUT /auth/api/permission/excludePath

- API 名称: update_exclude_path
- 功能说明：
	- 中文：更新权限排除路径
	- English：update permission exclude path

- input body:

``` json
{
    "permissionId":"5ea4f6608c165f702f5bd41e",
    "path":[
        "/path1",
        "/path2"
    ]
}
```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|permissionId|string|是|无|角色主键id|the permission primary key|
|path|string array|是|[]|路径列表|the path list|

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









