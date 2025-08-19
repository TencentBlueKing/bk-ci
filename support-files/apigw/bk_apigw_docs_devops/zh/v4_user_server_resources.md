### 请求方法/请求路径
#### GET /{apigwType}/v4/turbo/server_resources
### 资源描述
#### DevCloud专用资源统计查询接口
### 输入参数说明
#### Path参数
###### 无此参数
#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明   |
| --------- | ------- | --- | ------ |
| endDate   | String  |     | 截止统计日期 |
| pageNum   | integer |     | 页数     |
| pageSize  | integer |     | 每页多少条  |
| startDate | String  |     | 起始统计日期 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                                      | 说明               |
| ------- | ------------------------------------------------------------------------- | ---------------- |
| default | [ResponsePageProjectResourceUsageVO](#ResponsePageProjectResourceUsageVO) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?endDate={endDate}&pageNum={pageNum}&pageSize={pageSize}&startDate={startDate}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ {
      "bgId" : 0,
      "bgName" : "",
      "businessLineId" : 0,
      "businessLineName" : "",
      "centerId" : 0,
      "centerName" : "",
      "deptId" : 0,
      "deptName" : "",
      "engineCode" : "",
      "productId" : 0,
      "projectId" : "",
      "projectName" : "",
      "totalTimeWithCpu" : "number"
    } ],
    "totalPages" : 0
  },
  "message" : ""
}
```

### 相关模型数据
#### ResponsePageProjectResourceUsageVO
##### 

| 参数名称    | 参数类型                                                      | 必须  | 参数说明 |
| ------- | --------------------------------------------------------- | --- | ---- |
| code    | integer                                                   |     |      |
| data    | [PageProjectResourceUsageVO](#PageProjectResourceUsageVO) |     |      |
| message | string                                                    |     |      |

#### PageProjectResourceUsageVO
##### 分页数据包装模型

| 参数名称       | 参数类型                                                    | 必须  | 参数说明  |
| ---------- | ------------------------------------------------------- | --- | ----- |
| count      | integer                                                 | √   | 总记录行数 |
| page       | integer                                                 | √   | 第几页   |
| pageSize   | integer                                                 | √   | 每页多少条 |
| records    | List<[ProjectResourceUsageVO](#ProjectResourceUsageVO)> | √   | 数据    |
| totalPages | integer                                                 | √   | 总共多少页 |

#### ProjectResourceUsageVO
##### 数据

| 参数名称             | 参数类型    | 必须  | 参数说明 |
| ---------------- | ------- | --- | ---- |
| bgId             | integer |     |      |
| bgName           | string  |     |      |
| businessLineId   | integer |     |      |
| businessLineName | string  |     |      |
| centerId         | integer |     |      |
| centerName       | string  |     |      |
| deptId           | integer |     |      |
| deptName         | string  |     |      |
| engineCode       | string  |     |      |
| productId        | integer |     |      |
| projectId        | string  |     |      |
| projectName      | string  |     |      |
| totalTimeWithCpu | number  |     |      |

 
