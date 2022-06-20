## bkrepo 权限扩展接口


### 部门列表

- API:GET /auth/api/department/list
- API 名称: list_department
- 功能说明：
	- 中文：用户列表
	- English：list department

- input body:

``` json

```

- input 字段说明

- output:

```
{
    "code":0,
    "data":[
        {
            "name":"研发部",
            "id":"4"
        }
    ],
    "message":"",
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object | user data info |the info of user|
|traceId|string|请求跟踪id|the trace id|


- data 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|id | string | 用户id |the department id|
|name|string|用户名|the department name|




















