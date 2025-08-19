### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/operator_list
### 资源描述
#### 获取流水线操作人列表（分页）
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明  |
| ---------- | ------ | --- | ----- |
| pipelineId | String | √   | 流水线ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                  | 说明               |
| ------- | ------------------------------------- | ---------------- |
| default | [ResultListString](#ResultListString) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ "" ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListString
##### 数据返回包装模型

| 参数名称    | 参数类型         | 必须  | 参数说明 |
| ------- | ------------ | --- | ---- |
| data    | List<string> |     | 数据   |
| message | string       |     | 错误信息 |
| status  | integer      | √   | 状态码  |

 
