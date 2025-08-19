### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/credentials/credential
### 资源描述
#### 获取凭据
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
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                              | 说明               |
| ------- | ----------------------------------------------------------------- | ---------------- |
| default | [ResultCredentialWithPermission](#ResultCredentialWithPermission) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?credentialId={credentialId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "allowAcrossProject" : false,
    "createTime" : 0,
    "createUser" : "",
    "credentialId" : "",
    "credentialName" : "",
    "credentialRemark" : "",
    "credentialType" : "enum",
    "permissions" : {
      "delete" : false,
      "edit" : false,
      "use" : false,
      "view" : false
    },
    "updateUser" : "",
    "updatedTime" : 0,
    "v1" : "",
    "v2" : "",
    "v3" : "",
    "v4" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultCredentialWithPermission
##### 数据返回包装模型

| 参数名称    | 参数类型                                                  | 必须  | 参数说明 |
| ------- | ----------------------------------------------------- | --- | ---- |
| data    | [CredentialWithPermission](#CredentialWithPermission) |     |      |
| message | string                                                |     | 错误信息 |
| status  | integer                                               | √   | 状态码  |

#### CredentialWithPermission
##### 凭据-凭据内容和权限

| 参数名称               | 参数类型                                                                                                                                                                                                            | 必须  | 参数说明          |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------- |
| allowAcrossProject | boolean                                                                                                                                                                                                         | √   | 当前凭证是否允许跨项目使用 |
| createTime         | integer                                                                                                                                                                                                         | √   | 凭证创建时间        |
| createUser         | string                                                                                                                                                                                                          | √   | 凭证创建者         |
| credentialId       | string                                                                                                                                                                                                          | √   | 凭据ID          |
| credentialName     | string                                                                                                                                                                                                          |     | 凭据名称          |
| credentialRemark   | string                                                                                                                                                                                                          |     | 凭据描述          |
| credentialType     | ENUM(PASSWORD, ACCESSTOKEN, OAUTHTOKEN, USERNAME_PASSWORD, SECRETKEY, APPID_SECRETKEY, SSH_PRIVATEKEY, TOKEN_SSH_PRIVATEKEY, TOKEN_USERNAME_PASSWORD, COS_APPID_SECRETID_SECRETKEY_REGION, MULTI_LINE_PASSWORD) | √   | 凭据类型          |
| permissions        | [CredentialPermissions](#CredentialPermissions)                                                                                                                                                                 | √   |               |
| updateUser         | string                                                                                                                                                                                                          |     | 最后更新者         |
| updatedTime        | integer                                                                                                                                                                                                         | √   | 最后更新时间        |
| v1                 | string                                                                                                                                                                                                          | √   | 凭据内容          |
| v2                 | string                                                                                                                                                                                                          |     | 凭据内容          |
| v3                 | string                                                                                                                                                                                                          |     | 凭据内容          |
| v4                 | string                                                                                                                                                                                                          |     | 凭据内容          |

#### CredentialPermissions
##### 凭证-凭证权限

| 参数名称   | 参数类型    | 必须  | 参数说明 |
| ------ | ------- | --- | ---- |
| delete | boolean | √   | 删除权限 |
| edit   | boolean | √   | 编辑权限 |
| use    | boolean |     | 使用权限 |
| view   | boolean | √   | 查看权限 |

 
