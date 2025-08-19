### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/build_restart
### 资源描述
#### 取消并发起新构建
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明  |
| ---------- | ------ | --- | ----- |
| buildId    | String | √   | 构建ID  |
| pipelineId | String |     | 流水线ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                          | 说明               |
| ------- | ----------------------------- | ---------------- |
| default | [ResultString](#ResultString) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : "",
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultString
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | string  |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
