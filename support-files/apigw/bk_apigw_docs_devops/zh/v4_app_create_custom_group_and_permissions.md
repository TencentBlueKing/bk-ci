### 请求方法/请求路径
#### POST /{apigwType}/v4/auth/project/{projectId}/create_custom_group_and_permissions
### 资源描述
#### 创建自定义用户和权限
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目Id       |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称      | 参数类型                                          | 必须   |
| --------- | --------------------------------------------- | ---- |
| 自定义组创建请求体 | [CustomGroupCreateReq](#CustomGroupCreateReq) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultInteger](#ResultInteger) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "actions" : [ "" ],
  "groupDesc" : "",
  "groupName" : ""
}
```

### default 返回样例

```Json
{
  "data" : 0,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### CustomGroupCreateReq
##### 自定义组创建请求体

| 参数名称      | 参数类型         | 必须  | 参数说明 |
| --------- | ------------ | --- | ---- |
| actions   | List<string> | √   | 操作集合 |
| groupDesc | string       | √   | 组描述  |
| groupName | string       | √   | 组名称  |

#### ResultInteger
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | integer |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
