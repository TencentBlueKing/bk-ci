### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/project/get_projects_by_product_id
### 资源描述
#### 根据运营产品ID获取项目列表接口
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明 |
| --------- | ------- | --- | ---- |
| productId | integer | √   | 产品ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                    | 说明               |
| ------- | ------------------------------------------------------- | ---------------- |
| default | [ResultListProjectBaseInfo](#ResultListProjectBaseInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?productId={productId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : [ {
    "enabled" : false,
    "englishName" : "",
    "id" : 0,
    "projectName" : ""
  } ],
  "message" : "",
  "requestId" : "",
  "request_id" : "",
  "result" : false
}
```

### 相关模型数据
#### ResultListProjectBaseInfo
##### 数据返回包装模型

| 参数名称       | 参数类型                                      | 必须  | 参数说明 |
| ---------- | ----------------------------------------- | --- | ---- |
| code       | integer                                   | √   | 状态码  |
| data       | List<[ProjectBaseInfo](#ProjectBaseInfo)> |     | 数据   |
| message    | string                                    |     | 错误信息 |
| requestId  | string                                    |     | 请求ID |
| request_id | string                                    |     |      |
| result     | boolean                                   |     | 请求结果 |

#### ProjectBaseInfo
##### 项目基本信息

| 参数名称        | 参数类型    | 必须  | 参数说明 |
| ----------- | ------- | --- | ---- |
| enabled     | boolean |     | 是否启用 |
| englishName | string  | √   | 英文缩写 |
| id          | integer | √   | 主键ID |
| projectName | string  |     | 项目名称 |

 
