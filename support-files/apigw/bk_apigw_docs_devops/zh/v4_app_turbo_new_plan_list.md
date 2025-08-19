### 请求方法/请求路径
#### GET /{apigwType}/v4/turbo/projectId/{projectId}/turbo_plan_list
### 资源描述
#### 新版编译加速获取加速方案列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明  |
| --------- | ------- | --- | ----- |
| endTime   | String  |     | 结束日期  |
| pageNum   | integer |     | 页数    |
| pageSize  | integer |     | 每页多少条 |
| startTime | String  |     | 开始日期  |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                              | 说明               |
| ------- | ----------------------------------------------------------------- | ---------------- |
| default | [ResponsePageTurboPlanStatRowVO](#ResponsePageTurboPlanStatRowVO) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?endTime={endTime}&pageNum={pageNum}&pageSize={pageSize}&startTime={startTime}' \
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
      "engineCode" : "",
      "engineName" : "",
      "estimateTimeHour" : "",
      "executeCount" : 0,
      "executeTimeHour" : "",
      "instanceNum" : 0,
      "openStatus" : false,
      "planId" : "",
      "planName" : "",
      "topStatus" : "",
      "turboRatio" : ""
    } ],
    "totalPages" : 0
  },
  "message" : ""
}
```

### 相关模型数据
#### ResponsePageTurboPlanStatRowVO
##### 

| 参数名称    | 参数类型                                              | 必须  | 参数说明 |
| ------- | ------------------------------------------------- | --- | ---- |
| code    | integer                                           |     |      |
| data    | [PageTurboPlanStatRowVO](#PageTurboPlanStatRowVO) |     |      |
| message | string                                            |     |      |

#### PageTurboPlanStatRowVO
##### 分页数据包装模型

| 参数名称       | 参数类型                                            | 必须  | 参数说明  |
| ---------- | ----------------------------------------------- | --- | ----- |
| count      | integer                                         | √   | 总记录行数 |
| page       | integer                                         | √   | 第几页   |
| pageSize   | integer                                         | √   | 每页多少条 |
| records    | List<[TurboPlanStatRowVO](#TurboPlanStatRowVO)> | √   | 数据    |
| totalPages | integer                                         | √   | 总共多少页 |

#### TurboPlanStatRowVO
##### 数据

| 参数名称             | 参数类型    | 必须  | 参数说明 |
| ---------------- | ------- | --- | ---- |
| engineCode       | string  |     |      |
| engineName       | string  |     |      |
| estimateTimeHour | string  |     |      |
| executeCount     | integer |     |      |
| executeTimeHour  | string  |     |      |
| instanceNum      | integer |     |      |
| openStatus       | boolean |     |      |
| planId           | string  |     |      |
| planName         | string  |     |      |
| topStatus        | string  |     |      |
| turboRatio       | string  |     |      |

 
