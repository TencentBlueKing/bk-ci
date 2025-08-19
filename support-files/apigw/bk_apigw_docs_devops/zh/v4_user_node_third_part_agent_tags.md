### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/environment/fetch_agent_tag
### 资源描述
#### 查询项目标签和对应节点数
### 输入参数说明
#### Path参数
###### 无此参数
#### Query参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   | 项目ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                    | 说明               |
| ------- | --------------------------------------- | ---------------- |
| default | [ResultListNodeTag](#ResultListNodeTag) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?projectId={projectId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "canUpdate" : "enum",
    "tagAllowMulValue" : false,
    "tagKeyId" : 0,
    "tagKeyName" : "",
    "tagValues" : [ {
      "canUpdate" : "enum",
      "nodeCount" : 0,
      "tagValueId" : 0,
      "tagValueName" : ""
    } ]
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListNodeTag
##### 数据返回包装模型

| 参数名称    | 参数类型                      | 必须  | 参数说明 |
| ------- | ------------------------- | --- | ---- |
| data    | List<[NodeTag](#NodeTag)> |     | 数据   |
| message | string                    |     | 错误信息 |
| status  | integer                   | √   | 状态码  |

#### NodeTag
##### 节点标签

| 参数名称             | 参数类型                                | 必须  | 参数说明                |
| ---------------- | ----------------------------------- | --- | ------------------- |
| canUpdate        | ENUM(INTERNAL, TRUE, FALSE)         |     | 标签是否可以修改区分          |
| tagAllowMulValue | boolean                             | √   | 当前节点标签是否支持一个节点多个标签值 |
| tagKeyId         | integer                             | √   | 节点标签名ID             |
| tagKeyName       | string                              | √   | 节点标签名               |
| tagValues        | List<[NodeTagValue](#NodeTagValue)> | √   | 节点标签值               |

#### NodeTagValue
##### 节点标签值

| 参数名称         | 参数类型                        | 必须  | 参数说明       |
| ------------ | --------------------------- | --- | ---------- |
| canUpdate    | ENUM(INTERNAL, TRUE, FALSE) |     | 标签是否可以修改区分 |
| nodeCount    | integer                     |     | 标签包含的节点数量  |
| tagValueId   | integer                     | √   | 节点值ID      |
| tagValueName | string                      | √   | 节点值名       |

 
