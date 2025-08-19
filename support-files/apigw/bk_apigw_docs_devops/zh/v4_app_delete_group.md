### 请求方法/请求路径
#### DELETE /{apigwType}/v4/auth/project/{projectId}/delete_group/{resourceType}
### 资源描述
#### 刪除用户组
### 输入参数说明
#### Path参数

| 参数名称         | 参数类型   | 必须  | 参数说明       |
| ------------ | ------ | --- | ---------- |
| apigwType    | String | √   | apigw Type |
| projectId    | String | √   | 项目Id       |
| resourceType | String | √   | 资源类型       |

#### Query参数

| 参数名称    | 参数类型    | 必须  | 参数说明  |
| ------- | ------- | --- | ----- |
| groupId | integer | √   | 用户组ID |

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
curl -X DELETE '[请替换为上方API地址栏请求地址]?groupId={groupId}' \
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

 
