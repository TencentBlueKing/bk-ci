### 请求方法/请求路径
#### PUT /{apigwType}/v4/repositories/projects/{projectId}/{repositoryHashId}/pac/disable
### 资源描述
#### 关闭PAC
### 输入参数说明
#### Path参数

| 参数名称             | 参数类型   | 必须  | 参数说明        |
| ---------------- | ------ | --- | ----------- |
| apigwType        | String | √   | apigw Type  |
| projectId        | String | √   | 项目ID(项目英文名) |
| repositoryHashId | String | √   | 代码库哈希ID     |

#### Query参数
###### 无此参数
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
curl -X PUT '[请替换为上方API地址栏请求地址]' \
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

 
