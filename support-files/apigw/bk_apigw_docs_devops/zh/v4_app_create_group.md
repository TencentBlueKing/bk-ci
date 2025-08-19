### 请求方法/请求路径
#### POST /{apigwType}/v4/auth/project/{projectId}/create_group
### 资源描述
#### 创建自定义组(不包含权限，空权限组)
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

| 参数名称    | 参数类型                        | 必须   |
| ------- | --------------------------- | ---- |
| 添加用户组实体 | [GroupAddDTO](#GroupAddDTO) | true |

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
#### GroupAddDTO
##### 添加组DTO

| 参数名称      | 参数类型   | 必须  | 参数说明  |
| --------- | ------ | --- | ----- |
| groupDesc | string | √   | 组描述   |
| groupName | string | √   | 用户组名称 |

#### ResultInteger
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | integer |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
