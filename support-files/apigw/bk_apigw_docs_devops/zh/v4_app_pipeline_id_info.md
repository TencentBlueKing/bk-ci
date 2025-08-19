### 请求方法/请求路径
#### GET /{apigwType}/v4/permission/move/projects/{projectId}/pipeline_id_list
### 资源描述
#### 获取项目下pipelineId+自增id
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目Code     |

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

| HTTP代码  | 参数类型                                                  | 说明               |
| ------- | ----------------------------------------------------- | ---------------- |
| default | [ResultListPipelineIdInfo](#ResultListPipelineIdInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "id" : 0,
    "pipelineId" : ""
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListPipelineIdInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                                    | 必须  | 参数说明 |
| ------- | --------------------------------------- | --- | ---- |
| data    | List<[PipelineIdInfo](#PipelineIdInfo)> |     | 数据   |
| message | string                                  |     | 错误信息 |
| status  | integer                                 | √   | 状态码  |

#### PipelineIdInfo
##### 流水线id模型

| 参数名称       | 参数类型    | 必须  | 参数说明                          |
| ---------- | ------- | --- | ----------------------------- |
| id         | integer |     | 流水线自增ID，主要用于权限中心的资源ID，保证项目下唯一 |
| pipelineId | string  | √   | 流水线id，全局唯一                    |

 
