### 请求方法/请求路径
#### GET /{apigwType}/v4/auth/project/{projectId}/get_project_permission_info
### 资源描述
#### 获取项目权限信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                            | 说明               |
| ------- | --------------------------------------------------------------- | ---------------- |
| default | [ResultProjectPermissionInfoVO](#ResultProjectPermissionInfoVO) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "creator" : "",
    "members" : [ "" ],
    "owners" : [ "" ],
    "projectCode" : "",
    "projectName" : "",
    "project_id" : "",
    "project_name" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultProjectPermissionInfoVO
##### 数据返回包装模型

| 参数名称    | 参数类型                                                | 必须  | 参数说明 |
| ------- | --------------------------------------------------- | --- | ---- |
| data    | [ProjectPermissionInfoVO](#ProjectPermissionInfoVO) |     |      |
| message | string                                              |     | 错误信息 |
| status  | integer                                             | √   | 状态码  |

#### ProjectPermissionInfoVO
##### 项目权限信息

| 参数名称         | 参数类型         | 必须  | 参数说明 |
| ------------ | ------------ | --- | ---- |
| creator      | string       | √   | 创建人  |
| members      | List<string> | √   | 项目成员 |
| owners       | List<string> | √   | 管理员  |
| projectCode  | string       | √   | 项目ID |
| projectName  | string       | √   | 项目名称 |
| project_id   | string       |     |      |
| project_name | string       |     |      |

 
