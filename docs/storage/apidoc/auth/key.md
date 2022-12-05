## bkrepo 平台密钥相关接口

### 创建密钥

- API: POST /auth/api/key/create
- API 名称: create_key
- 功能说明：
  - 中文：创建密钥
  - English：create key

- input body:

``` 
name: string
key: string
```

- input 字段说明

| 字段 | 类型   | 是否必须 | 默认值 | 说明     | Description   |
| ---- | ------ | -------- | ------ | -------- | ------------- |
| name | string | 是       | 无     | 密钥名称 | the key name  |
| key  | string | 是       | 无     | 密钥     | the key value |

- output:

```
{
    "code":0,
    "data": null,
    "message":"string",
    "traceId":"string"
}


```

- output 字段说明

| 字段    | 类型           | 说明                                    | Description               |
| ------- | -------------- | --------------------------------------- | ------------------------- |
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message | result message | 错误消息                                | the failure message       |
| data    | object         | result data                             | the data for response     |
| traceId | string         | 请求跟踪id                              | the trace id              |


### 删除密钥

- API: DELETE /auth/api/key/delete/{id}
- API 名称: delete_key
- 功能说明：
  - 中文：删除密钥
  - English：delete key

- input body:

``` json

```

- input 字段说明

| 字段 | 类型   | 是否必须 | 默认值 | 说明   | Description |
| ---- | ------ | -------- | ------ | ------ | ----------- |
| id   | string | 是       | 无     | 密钥ID | the key id  |

- output:

```
{
    "code":0,
    "data":null,
    "message":"",
    "traceId":""
}

```

- output 字段说明

| 字段    | 类型           | 说明                                    | Description               |
| ------- | -------------- | --------------------------------------- | ------------------------- |
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message | result message | 错误消息                                | the failure message       |
| data    | object         | result data                             | the data for response     |
| traceId | string         | 请求跟踪id                              | the trace id              |

### 查询密钥列表

- API: GET /auth/api/key/list
- API 名称: list_key
- 功能说明：
  - 中文：查询所有访问账号
  - English：list key

- input body:

``` json

```

- input 字段说明


- output:

```
{
    "code": 0,
    "message": null,
    "data": [
        {
            "id": "string",
            "name": "string",
            "fingerprint": "6d:3c:a7:52:ec:4d:ef:ef:6d:23:15:43:c6:ff:25:27",
            "createAt": "2021-09-03T15:55:01.643"
        },
        {
            "id": "string",
            "name": "string",
            "fingerprint": "af:f6:f4:f8:38:a1:1c:8d:af:56:25:ee:b0:15:93:bc",
            "createAt": "2021-09-03T15:57:27.554"
        }
    ],
    "traceId": ""
}

```

- output 字段说明

| 字段    | 类型           | 说明                                    | Description               |
| ------- | -------------- | --------------------------------------- | ------------------------- |
| code    | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message | result message | 错误消息                                | the failure message       |
| data    | object array   | result data                             | the data for response     |
| traceId | string         | 请求跟踪id                              | the trace id              |

