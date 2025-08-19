### 请求方法/请求路径
#### GET /{apigwType}/v4/auth/validate/projects/{projectId}/check_project_users
### 资源描述
#### 判断是否某个项目中某个组角色的成员
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目Code     |

#### Query参数

| 参数名称  | 参数类型                                                                                                                                                     | 必须  | 参数说明  |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ----- |
| group | ENUM(VISITOR, CIADMIN, MANAGER, DEVELOPER, MAINTAINER, TESTER, PM, QC, CI_MANAGER, GRADE_ADMIN, CGS_MANAGER, RESOURCE_MANAGER, EDITOR, EXECUTOR, VIEWER) |     | 用户组类型 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?group={group}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
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
#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
