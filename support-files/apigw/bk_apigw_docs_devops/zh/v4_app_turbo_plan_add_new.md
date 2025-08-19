### 请求方法/请求路径
#### POST /{apigwType}/v4/turbo/projectId/{projectId}/addTurboPlan
### 资源描述
#### 新增加速方案
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明   |
| --------- | ------ | --- | ------ |
| projectId | String | √   | 蓝盾项目id |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称         | 参数类型                              | 必须   |
| ------------ | --------------------------------- | ---- |
| 新增加速方案请求数据信息 | [TurboPlanModel](#TurboPlanModel) | true |

#### 响应参数

| HTTP代码  | 参数类型                              | 说明               |
| ------- | --------------------------------- | ---------------- |
| default | [ResponseString](#ResponseString) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "configParam" : {
    "string" : "Any 任意类型，参照实际请求或返回"
  },
  "desc" : "",
  "engineCode" : "",
  "openStatus" : false,
  "planName" : "",
  "projectId" : "",
  "whiteList" : ""
}
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : "",
  "message" : ""
}
```

### 相关模型数据
#### TurboPlanModel
##### 

| 参数名称        | 参数类型             | 必须  | 参数说明 |
| ----------- | ---------------- | --- | ---- |
| configParam | Map<String, Any> | √   |      |
| desc        | string           |     |      |
| engineCode  | string           |     |      |
| openStatus  | boolean          | √   |      |
| planName    | string           |     |      |
| projectId   | string           |     |      |
| whiteList   | string           |     |      |

#### ResponseString
##### 

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| code    | integer |     |      |
| data    | string  |     |      |
| message | string  |     |      |

 
