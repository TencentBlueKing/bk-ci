### 请求方法/请求路径
#### GET /{apigwType}/v4/turbo/projectId/{projectId}/turbo_plan_detail
### 资源描述
#### 新版编译加速获取方案详情
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明          |
| --------- | ------ | --- | ------------- |
| projectId | String | √   | 蓝盾项目ID(项目英文名) |

#### Query参数

| 参数名称   | 参数类型   | 必须  | 参数说明 |
| ------ | ------ | --- | ---- |
| planId | String | √   | 方案id |

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
| default | [ResponseTurboPlanDetailVO](#ResponseTurboPlanDetailVO) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?planId={planId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : {
    "configParam" : {
      "string" : "Any 任意类型，参照实际请求或返回"
    },
    "createdBy" : "",
    "createdDate" : "string",
    "desc" : "",
    "engineCode" : "",
    "engineName" : "",
    "openStatus" : false,
    "planId" : "",
    "planName" : "",
    "projectId" : "",
    "updatedBy" : "",
    "updatedDate" : "string",
    "whiteList" : ""
  },
  "message" : ""
}
```

### 相关模型数据
#### ResponseTurboPlanDetailVO
##### 

| 参数名称    | 参数类型                                    | 必须  | 参数说明 |
| ------- | --------------------------------------- | --- | ---- |
| code    | integer                                 |     |      |
| data    | [TurboPlanDetailVO](#TurboPlanDetailVO) |     |      |
| message | string                                  |     |      |

#### TurboPlanDetailVO
##### 

| 参数名称        | 参数类型             | 必须  | 参数说明 |
| ----------- | ---------------- | --- | ---- |
| configParam | Map<String, Any> |     |      |
| createdBy   | string           |     |      |
| createdDate | string           |     |      |
| desc        | string           |     |      |
| engineCode  | string           |     |      |
| engineName  | string           |     |      |
| openStatus  | boolean          |     |      |
| planId      | string           |     |      |
| planName    | string           |     |      |
| projectId   | string           |     |      |
| updatedBy   | string           |     |      |
| updatedDate | string           |     |      |
| whiteList   | string           |     |      |

 
