### 请求方法/请求路径
#### POST /{apigwType}/v4/projects/{projectId}/build_variables
### 资源描述
#### 获取构建中的变量值(注意：变量具有时效性，只能获取最近一个月的任务数据)
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
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

| 参数名称  | 参数类型         | 必须   |
| ----- | ------------ | ---- |
| 变量名列表 | List<string> | true |

#### 响应参数

| HTTP代码  | 参数类型                                            | 说明               |
| ------- | ----------------------------------------------- | ---------------- |
| default | [ResultMapStringString](#ResultMapStringString) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
[ "" ]
```

### default 返回样例

```Json
{
  "data" : {
    "string" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultMapStringString
##### 数据返回包装模型

| 参数名称    | 参数类型                | 必须  | 参数说明 |
| ------- | ------------------- | --- | ---- |
| data    | Map<String, string> |     | 数据   |
| message | string              |     | 错误信息 |
| status  | integer             | √   | 状态码  |

 
