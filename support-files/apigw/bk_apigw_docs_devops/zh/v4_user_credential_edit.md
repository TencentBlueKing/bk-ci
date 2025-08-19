### 请求方法/请求路径
#### PUT /{apigwType}/v4/projects/{projectId}/credentials/credential
### 资源描述
#### 编辑凭据
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称         | 参数类型   | 必须  | 参数说明 |
| ------------ | ------ | --- | ---- |
| credentialId | String | √   | 凭据ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称 | 参数类型                                  | 必须   |
| ---- | ------------------------------------- | ---- |
| 凭据   | [CredentialUpdate](#CredentialUpdate) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X PUT '[请替换为上方API地址栏请求地址]?credentialId={credentialId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### PUT 请求样例

```Json
{
  "credentialName" : "",
  "credentialRemark" : "",
  "credentialType" : "enum",
  "v1" : "",
  "v2" : "",
  "v3" : "",
  "v4" : ""
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
#### CredentialUpdate
##### 凭据-更新时内容

| 参数名称             | 参数类型                                                                                                                                                                                                            | 必须  | 参数说明 |
| ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ---- |
| credentialName   | string                                                                                                                                                                                                          |     | 凭据名称 |
| credentialRemark | string                                                                                                                                                                                                          |     | 凭据描述 |
| credentialType   | ENUM(PASSWORD, ACCESSTOKEN, OAUTHTOKEN, USERNAME_PASSWORD, SECRETKEY, APPID_SECRETKEY, SSH_PRIVATEKEY, TOKEN_SSH_PRIVATEKEY, TOKEN_USERNAME_PASSWORD, COS_APPID_SECRETID_SECRETKEY_REGION, MULTI_LINE_PASSWORD) | √   | 凭据类型 |
| v1               | string                                                                                                                                                                                                          | √   | 凭据内容 |
| v2               | string                                                                                                                                                                                                          |     | 凭据内容 |
| v3               | string                                                                                                                                                                                                          |     | 凭据内容 |
| v4               | string                                                                                                                                                                                                          |     | 凭据内容 |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
