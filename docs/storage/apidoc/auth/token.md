## bkrepo token相关接口

### 新增用户token

- API:POST  /auth/api/user/token/{uid}/{name}?expiredAt=2019-12-21T09:46:37.877Z&projectId=aaa
- API 名称: add_user_token
- 功能说明：
	- 中文：新增用户token
	- English：add user token

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|name|string|是|无|token名|the token name|
|uid|string|是|无|用户id|the user id|
|expiredAt|datetime|否|无|过期时间|expiredAt|
|projectId|aaa|否|无|项目ID|projectId|

- output:

```
{
    "code":0,
    "data":{
        "createdAt":"2019-12-21T09:46:37.877Z",
        "expiredAt":"2019-12-21T09:46:37.877Z",
        "id":"abcd",
        "name":"abcd"
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


- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|createdAt|time|创建时间|create time|
|expiredAt|time|过期时间,null标识永久 |expired time |
|name | string | token name|the name of token|
|id|string|token id |the id of token|

### 用户token列表

- API:POST  /auth/api/user/list/token/{uid}
- API 名称: list_user_token
- 功能说明：
	- 中文：新增用户token
	- English：add user token

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
    "data":[
        {
            "createdAt":"2019-12-21T09:46:37.877Z",
            "expiredAt":"2019-12-21T09:46:37.877Z",
            "name":"token1"
        },
        {
            "createdAt":"2019-12-21T09:46:37.877Z",
            "expiredAt":null,
            "name":"token2"
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
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|


- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|createdAt|time|创建时间|create time|
|expiredAt|time|过期时间 |expired time |
|name | string | token name|the name of token|
|id|string|token id |the id of token|

### 删除用户token

- API:DELETE  /auth/api/user/token/{uid}/{name}
- API 名称: delete_user_token
- 功能说明：
	- 中文：删除用户token
	- English：delete user token

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|
|name|string|是|无|用户token|the user token|

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
                "name":"string"
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

### 校验用户token

- API:POST /auth/api/user/login?uid=owen&token=blueking
- API 名称: check_user_token
- 功能说明：
	- 中文：校验用户token
	- English：check user token

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|
|token|string|是|无|用户token|the user token|

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
|message|string|错误消息,或者用户token |the failure message,or bkrepo token |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 获取用户信息

- API:GET /auth/api/user/info
- API 名称: check_user_token
- 功能说明：
	- 中文：校验用户token
	- English：check user token

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|uid|string|是|无|用户id|the user id|
|bkrepo_ticket|cookie|是|无|用户token|the user token|

- output:

```
{
    "code":0,
    "message":null,
    "data":{
        "userId":"owenlxu2"
    },
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|string|错误消息,或者用户token |the failure message,or bkrepo token |
|data | bool | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|


- data字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|userId | string | 用户id |the user Id|
|bkrepo_ticket|string|请求跟踪id|the bkrepo ticket|
