### 请求方法/请求路径
#### POST /{apigwType}/v4/auth/oauth2/endpoint/getAccessToken
### 资源描述
#### oauth2获取accessToken
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称                          | 参数类型   | 必须  | 参数说明             |
| ----------------------------- | ------ | --- | ---------------- |
| Content-Type                  | string | √   | application/json |
| X-DEVOPS-UID                  | string | √   | 用户名              |
| X-DEVOPS-OAUTH2-CLIENT-ID     | String | √   | 客户端id            |
| X-DEVOPS-OAUTH2-CLIENT-SECRET | String | √   | 客户端秘钥            |

#### Body参数

| 参数名称               | 参数类型                                                  | 必须   |
| ------------------ | ----------------------------------------------------- | ---- |
| oauth2获取token请求报文体 | [Oauth2AccessTokenRequest](#Oauth2AccessTokenRequest) | true |

#### 响应参数

| HTTP代码  | 参数类型                                                    | 说明               |
| ------- | ------------------------------------------------------- | ---------------- |
| default | [ResultOauth2AccessTokenVo](#ResultOauth2AccessTokenVo) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' \
-H 'X-DEVOPS-OAUTH2-CLIENT-ID: 客户端id' \
-H 'X-DEVOPS-OAUTH2-CLIENT-SECRET: 客户端秘钥' 
```

### POST 请求样例

```Json
{
  "grantType" : "enum"
}
```

### default 返回样例

```Json
{
  "data" : {
    "accessToken" : "",
    "expiredTime" : 0,
    "refreshToken" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### Oauth2AccessTokenRequest
##### oauth2获取token请求报文体

| 参数名称      | 参数类型   | 必须  | 参数说明                                                                                                      |
| --------- | ------ | --- | --------------------------------------------------------------------------------------------------------- |
| grantType | string | √   | 用于指定实现某一多态类, 可选[Oauth2AuthorizationCodeRequest, Oauth2PassWordRequest, Oauth2RefreshTokenRequest],具体实现见下方 |

#### Oauth2AuthorizationCodeRequest
 *多态基类 <Oauth2AccessTokenRequest> 的实现处, 其中当字段 grantType = [AUTHORIZATION_CODE] 时指定为该类实现*
 

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| grantType | string | 必须是 | 多态类实现       | AUTHORIZATION_CODE |
| code      | string | √   | 授权码,用于授权码模式 |

#### Oauth2PassWordRequest
 *多态基类 <Oauth2AccessTokenRequest> 的实现处, 其中当字段 grantType = [PASS_WORD] 时指定为该类实现*
 

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| grantType | string | 必须是 | 多态类实现       | PASS_WORD |
| passWord  | string |     | 密码，用于密码模式   |
| userName  | string |     | 账号名称，用于密码模式 |

#### Oauth2RefreshTokenRequest
 *多态基类 <Oauth2AccessTokenRequest> 的实现处, 其中当字段 grantType = [REFRESH_TOKEN] 时指定为该类实现*
 

| 参数名称         | 参数类型   | 必须  | 参数说明          |
| ------------ | ------ | --- | ------------- |
| grantType    | string | 必须是 | 多态类实现         | REFRESH_TOKEN |
| refreshToken | string | √   | 刷新码,用于刷新授权码模式 |

#### ResultOauth2AccessTokenVo
##### 数据返回包装模型

| 参数名称    | 参数类型                                        | 必须  | 参数说明 |
| ------- | ------------------------------------------- | --- | ---- |
| data    | [Oauth2AccessTokenVo](#Oauth2AccessTokenVo) |     |      |
| message | string                                      |     | 错误信息 |
| status  | integer                                     | √   | 状态码  |

#### Oauth2AccessTokenVo
##### oauth2获取token请求返回体

| 参数名称         | 参数类型    | 必须  | 参数说明            |
| ------------ | ------- | --- | --------------- |
| accessToken  | string  | √   | accessToken     |
| expiredTime  | integer | √   | accessToken过期时间 |
| refreshToken | string  |     | refreshToken    |

 
