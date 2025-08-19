### 请求方法/请求路径
#### POST /{apigwType}/v4/turbo/manualUpload
### 资源描述
#### 手动上报项目资源统计数据
### 输入参数说明
#### Path参数
###### 无此参数
#### Query参数
###### 无此参数
#### Header参数

| 参数名称                | 参数类型   | 必须  | 参数说明             |
| ------------------- | ------ | --- | ---------------- |
| Content-Type        | string | √   | application/json |
| X-DEVOPS-UID        | string | √   | 用户名              |
| X-DEVOPS-PROJECT-ID | String |     | 项目id             |

#### Body参数

| 参数名称   | 参数类型                                        | 必须  |
| ------ | ------------------------------------------- | --- |
| 待上报的数据 | [ResourceCostSummary](#ResourceCostSummary) |     |

#### 响应参数

| HTTP代码  | 参数类型                                | 说明               |
| ------- | ----------------------------------- | ---------------- |
| default | [ResponseBoolean](#ResponseBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' \
-H 'X-DEVOPS-PROJECT-ID: 项目id' 
```

### POST 请求样例

```Json
{
  "bills" : [ {
    "bgName" : "",
    "bg_name" : "",
    "costDate" : "",
    "cost_date" : "",
    "flag" : 0,
    "kind" : "",
    "name" : "",
    "projectId" : "",
    "project_id" : "",
    "serviceType" : "",
    "service_type" : "",
    "usage" : ""
  } ],
  "dataSourceName" : "",
  "data_source_name" : "",
  "is_overwrite" : false,
  "month" : "",
  "overwrite" : false
}
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : false,
  "message" : ""
}
```

### 相关模型数据
#### ResourceCostSummary
##### 

| 参数名称             | 参数类型                                                  | 必须  | 参数说明 |
| ---------------- | ----------------------------------------------------- | --- | ---- |
| bills            | List<[ProjectResourceCostVO](#ProjectResourceCostVO)> |     |      |
| dataSourceName   | string                                                |     |      |
| data_source_name | string                                                |     |      |
| is_overwrite     | boolean                                               |     |      |
| month            | string                                                |     |      |
| overwrite        | boolean                                               |     |      |

#### ProjectResourceCostVO
##### 

| 参数名称         | 参数类型    | 必须  | 参数说明 |
| ------------ | ------- | --- | ---- |
| bgName       | string  |     |      |
| bg_name      | string  |     |      |
| costDate     | string  |     |      |
| cost_date    | string  |     |      |
| flag         | integer |     |      |
| kind         | string  |     |      |
| name         | string  |     |      |
| projectId    | string  |     |      |
| project_id   | string  |     |      |
| serviceType  | string  |     |      |
| service_type | string  |     |      |
| usage        | string  |     |      |

#### ResponseBoolean
##### 

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| code    | integer |     |      |
| data    | boolean |     |      |
| message | string  |     |      |

 
