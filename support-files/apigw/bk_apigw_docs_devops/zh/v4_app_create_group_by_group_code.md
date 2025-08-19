### 请求方法/请求路径
#### POST /{apigwType}/v4/auth/project/{projectId}/create_group_by_group_code/{resourceType}
### 资源描述
#### 根据groupCode添加用户组
### 输入参数说明
#### Path参数

| 参数名称         | 参数类型   | 必须  | 参数说明       |
| ------------ | ------ | --- | ---------- |
| apigwType    | String | √   | apigw Type |
| projectId    | String | √   | 项目Id       |
| resourceType | String | √   | 资源类型       |

#### Query参数

| 参数名称      | 参数类型                                                                                                                                                     | 必须  | 参数说明                     |
| --------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------------------------ |
| groupCode | ENUM(VISITOR, CIADMIN, MANAGER, DEVELOPER, MAINTAINER, TESTER, PM, QC, CI_MANAGER, GRADE_ADMIN, CGS_MANAGER, RESOURCE_MANAGER, EDITOR, EXECUTOR, VIEWER) | √   | 用户组code,CI管理员为CI_MANAGER |
| groupDesc | String                                                                                                                                                   | √   | 用户组描述                    |
| groupName | String                                                                                                                                                   | √   | 用户组名称                    |

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
| default | [ResultInteger](#ResultInteger) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?groupCode={groupCode}&groupDesc={groupDesc}&groupName={groupName}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
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
#### ResultInteger
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | integer |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
