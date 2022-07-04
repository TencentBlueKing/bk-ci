## bkrepo 平台账号相关接口

### 创建访问账号

- API: POST /auth/api/account/create
- API 名称: create_account
- 功能说明：
	- 中文：创建访问账号
	- English：create account

- input body:

``` json
{
  "appId": "string",
  "locked": false,
  "authorizationGrantTypes": ["PLATFORM","AUTHORIZATION_CODE"],
  "homepageUrl":"http://localhost",
  "redirectUri":"http://localhost/redirect",
  "scope": ["PROJECT","REPO","NODE"],
  "avatarUrl": "string",
  "description": "string"
}
```
- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|appId|string|是|无|应用ID|the application id|
|locked|bool|否|false|是否锁定|the account status|
|authorizationGrantTypes|set<enum>|是|无|认证授权方式，[PLATFORM,AUTHORIZATION_CODE]|the account authorization grant types|
|homepageUrl|string|授权码模式时必须|无|账号主页地址|the account homepage url|
|redirectUri|string|授权码模式时必须|无|回调地址|the redirect uri|
|avatarUrl|string|否|无|账号图标地址|the avatar url|
|scope|set<enum>|授权码模式时必须|无|权限范围[SYSTEM,PROJECT,REPO,NODE]|the account permission scope|
|description|string|否|无|账号描述信息|the account description|

- output:

```json
{
    "code": 0,
    "message": null,
    "data": {
        "id": "string",
        "appId": "string",
        "locked": false,
        "credentials": [
            {
                "accessKey": "string",
                "secretKey": "string",
                "createdAt": "2021-08-30T18:28:49.365",
                "status": "ENABLE",
                "authorizationGrantType": "PLATFORM"
            },
            {
                "accessKey": "string",
                "secretKey": "string",
                "createdAt": "2021-08-30T18:28:49.365",
                "status": "ENABLE",
                "authorizationGrantType": "AUTHORIZATION_CODE"
            }
        ],
        "owner": "string",
        "authorizationGrantTypes": [
            "PLATFORM",
            "AUTHORIZATION_CODE"
        ],
        "homepageUrl": "http://localhost",
        "redirectUri": "http://localhost/redirect",
        "avatarUrl": "string",
        "scope": [
            "PROJECT",
            "REPO",
            "NODE"
        ],
        "description": "string",
        "createdDate": "2021-08-30T18:28:49.366",
        "lastModifiedDate": "2021-08-30T18:28:49.366"
    },
    "traceId": ""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

- credentials字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|accessKey|string|accessKey |accessKey|
|secretKey|string|secretKey|secretKey |
|createdAt | date time | 创建时间 |the create time|
|status|ENUM|[ENABLE,DISABLE]|[ENABLE,DISABLE]|
|authorizationGrantType|ENUM|[PLATFORM,AUTHORIZATION_CODE]|[PLATFORM,AUTHORIZATION_CODE]|


### 删除访问账号

- API: DELETE /auth/api/account/delete/{appId}
- API 名称: delete_account
- 功能说明：
	- 中文：删除访问账号
	- English：delete account

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|appId|string|是|无|应用ID|the application id|

- output:

```json
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
|data | object | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 卸载访问账号

- API: DELETE /auth/api/account/uninstall/{appId}
- API 名称: uninstall_account
- 功能说明：
  - 中文：卸载访问账号
  - English：uninstall account

- input body:

``` json

```

- input 字段说明

| 字段  | 类型   | 是否必须 | 默认值 | 说明   | Description        |
| ----- | ------ | -------- | ------ | ------ | ------------------ |
| appId | string | 是       | 无     | 应用ID | the application id |

- output:

```json
{
    "code":0,
    "data":true,
    "message":"",
    "traceId":""
}
```

- output 字段说明

| 字段    | 类型           | 说明                                    | Description               |
| ------- | -------------- | --------------------------------------- | ------------------------- |
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message | result message | 错误消息                                | the failure message       |
| data    | object         | result data                             | the data for response     |
| traceId | string         | 请求跟踪id                              | the trace id              |

### 

### 更新访问账号

- API: PUT /auth/api/account/update
- API 名称: update_account
- 功能说明：
	- 中文：更新访问账号
	- English：update account

- input body:

``` json
{
  "appId": "string",
  "locked": false,
  "authorizationGrantTypes": ["PLATFORM","AUTHORIZATION_CODE"],
  "homepageUrl":"http://localhost",
  "redirectUri":"http://localhost/redirect",
  "scope": ["PROJECT","REPO","NODE"],
  "avatarUrl": "string",
  "description": "string"
}
```
- input 字段说明

  | 字段                    | 类型      | 是否必须             | 默认值 | 说明                                        | Description                           |
  | ----------------------- | --------- | -------------------- | ------ | ------------------------------------------- | ------------------------------------- |
  | appId                   | string    | 是                   | 无     | 应用ID                                      | the application id                    |
  | locked                  | bool      | 否                   | false  | 是否锁定                                    | the account status                    |
  | authorizationGrantTypes | set<enum> | 是                   | 无     | 认证授权方式，[PLATFORM,AUTHORIZATION_CODE] | the account authorization grant types |
  | homepageUrl             | string    | 新增授权码模式时必须 | 无     | 账号主页地址                                | the account homepage url              |
  | redirectUri             | string    | 新增授权码模式时必须 | 无     | 回调地址                                    | the redirect uri                      |
  | avatarUrl               | string    | 否                   | 无     | 账号图标地址                                | the avatar url                        |
  | scope                   | set<enum> | 新增授权码模式时必须 | 无     | 权限范围[SYSTEM,PROJECT,REPO,NODE]          | the account permission scope          |
  | description             | string    | 否                   | 无     | 账号描述信息                                | the account description               |

- output:

```json
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
|data | object | result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 查询所有访问账号（管理员权限）

- API: GET /auth/api/account/list
- API 名称: list_account
- 功能说明：
	- 中文：查询所有访问账号
	- English：list account

- input body:

``` json

```
- input 字段说明


- output:

```json
{
    "code":0,
    "data":[
        {
            "appId":"bkdevops",
            "credentials":[
                {
                    "accessKey":"aaaassveee",
                    "createdAt":"2019-12-22T10:33:11.957Z",
                    "secretKey":"ssdverrrr",
                    "status":"ENABLE"
                }
            ],
            "locked":true
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
|data | object array| result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 查询所有拥有的访问账号

- API: GET /auth/api/account/own/list
- API 名称: list_own_account
- 功能说明：
  - 中文：查询所有拥有的访问账号
  - English：list own account

- input body:

``` json

```

- input 字段说明


- output:

```json
{
    "code":0,
    "data":[
        {
            "appId":"bkdevops",
            "credentials":[
                {
                    "accessKey":"aaaassveee",
                    "createdAt":"2019-12-22T10:33:11.957Z",
                    "secretKey":"ssdverrrr",
                    "status":"ENABLE"
                }
            ],
            "locked":true
        }
    ],
    "message":"",
    "traceId":""
}


```

- output 字段说明

| 字段    | 类型           | 说明                                    | Description               |
| ------- | -------------- | --------------------------------------- | ------------------------- |
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message | result message | 错误消息                                | the failure message       |
| data    | object array   | result data                             | the data for response     |
| traceId | string         | 请求跟踪id                              | the trace id              |

### 查询所有已授权的访问账号

- API: GET /auth/api/account/authorized/list
- API 名称: list_authorized_account
- 功能说明：
  - 中文：查询所有已授权的访问账号
  - English：list authorized account

- input body:

``` json

```

- input 字段说明


- output:

```json
{
    "code":0,
    "data":[
        {
            "appId":"bkdevops",
            "credentials":[
                {
                    "accessKey":"aaaassveee",
                    "createdAt":"2019-12-22T10:33:11.957Z",
                    "secretKey":"ssdverrrr",
                    "status":"ENABLE"
                }
            ],
            "locked":true
        }
    ],
    "message":"",
    "traceId":""
}


```

- output 字段说明

| 字段    | 类型           | 说明                                    | Description               |
| ------- | -------------- | --------------------------------------- | ------------------------- |
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message | result message | 错误消息                                | the failure message       |
| data    | object array   | result data                             | the data for response     |
| traceId | string         | 请求跟踪id                              | the trace id              |

### 

### 获取账号下的ak/sk对

- API: GET /auth/api/account/credential/list/{appId}
- API 名称: list_account_credential
- 功能说明：
	- 中文：查询账号的认证方式
	- English：list account credential

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
            "accessKey":"aaaa",
            "createdAt":"2019-12-22T10:33:11.929Z",
            "secretKey":"vbbbbb",
            "status":"ENABLE"
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
|data | object array| result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 创建账号下的ak/sk对

- API: POST /auth/api/account/credential/{appId}/{type}
- API 名称: create_account_credential
- 功能说明：
	- 中文：查询账号的认证方式
	- English：create account credential

- input body:

``` json

```
- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|appId|string|是|无|应用ID|the application id|
|Type|enum|是|无|认证授权方式[PLATFORM,AUTHORIZATION_CODE]|the authorization grant type|

- output:

```json
{
    "code":0,
    "data":[
        {
            "accessKey":"aaaaa",
            "createdAt":"2019-12-22T10:50:37.073Z",
            "secretKey":"cccccc",
            "status":"ENABLE",
            "authorizationGrantType": "PLATFORM"
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
|data | object array| result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 删除账号下的ak/sk对

- API: DELETE /auth/api/account/credential/{appId}/{accessKey}
- API 名称: delete_account_credential
- 功能说明：
	- 中文：删除账号下的ak/sk对
	- English：delete account credential

- input body:

``` json

```
- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|appId|string|是|无|应用ID|the application id|
|accessKey|string|是|无|accessKey|accessKey|

- output:

```json
{
    "code":0,
    "data": true,
    "message":"",
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object array| result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 校验账号下的ak/sk对

- API: GET /auth/api/account/credential/{appId}/{accessKey}/{secretKey}
- API 名称: check_account_credential
- 功能说明：
	- 中文：校验账号下的ak/sk对
	- English：check account credential

- input body:

``` json

```
- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|appId|string|是|无|应用ID|the application id|
|accessKey|string|是|无|accessKey|accessKey|
|secretKey|string|是|无|secretKey|secretKey|

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
|data | object array| result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

### 更新账号下的ak/sk对状态

- API: PUT /auth/api/account/credential/{appId}/{accessKey}/{status}
- API 名称: update_account_credential_status
- 功能说明：
	- 中文：更新账号下的ak/sk对状态
	- English：update account credential status

- input body:

``` json

```
- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|appId|string|是|无|应用ID|the application id|
|accessKey|string|是|无|accessKey|accessKey|
|status|ENUM|是|无|[ ENABLE, DISABLE]|[ ENABLE, DISABLE]|

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
|data | object array| result data |the data for response|
|traceId|string|请求跟踪id|the trace id|

