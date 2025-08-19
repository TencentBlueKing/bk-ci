### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/creator_list
### 资源描述
#### 获取流水线编排创建人列表（分页）
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明  |
| ---------- | ------- | --- | ----- |
| page       | integer |     | 第几页   |
| pageSize   | integer |     | 每页多少条 |
| pipelineId | String  | √   | 流水线ID |

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
| default | [ResultPageString](#ResultPageString) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?page={page}&pageSize={pageSize}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ "" ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageString
##### 数据返回包装模型

| 参数名称    | 参数类型                      | 必须  | 参数说明 |
| ------- | ------------------------- | --- | ---- |
| data    | [PageString](#PageString) |     |      |
| message | string                    |     | 错误信息 |
| status  | integer                   | √   | 状态码  |

#### PageString
##### 分页数据包装模型

| 参数名称       | 参数类型         | 必须  | 参数说明  |
| ---------- | ------------ | --- | ----- |
| count      | integer      | √   | 总记录行数 |
| page       | integer      | √   | 第几页   |
| pageSize   | integer      | √   | 每页多少条 |
| records    | List<string> | √   | 数据    |
| totalPages | integer      | √   | 总共多少页 |

 
