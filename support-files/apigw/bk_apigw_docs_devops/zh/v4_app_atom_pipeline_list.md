### 请求方法/请求路径
#### GET /{apigwType}/v4/atoms/atom_pipelines
### 资源描述
#### 根据插件代码获取使用的流水线详情
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数

| 参数名称     | 参数类型    | 必须  | 参数说明              |
| -------- | ------- | --- | ----------------- |
| atomCode | String  | √   | 插件代码              |
| page     | integer |     | 第几页               |
| pageSize | integer |     | 每页条数(默认20, 最大100) |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                              | 说明               |
| ------- | ------------------------------------------------- | ---------------- |
| default | [ResultPageAtomPipeline](#ResultPageAtomPipeline) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?atomCode={atomCode}&page={page}&pageSize={pageSize}' \
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
    "records" : [ {
      "atomVersion" : "",
      "bgName" : "",
      "centerName" : "",
      "deptName" : "",
      "pipelineId" : "",
      "pipelineName" : "",
      "projectCode" : "",
      "projectName" : ""
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageAtomPipeline
##### 数据返回包装模型

| 参数名称    | 参数类型                                  | 必须  | 参数说明 |
| ------- | ------------------------------------- | --- | ---- |
| data    | [PageAtomPipeline](#PageAtomPipeline) |     |      |
| message | string                                |     | 错误信息 |
| status  | integer                               | √   | 状态码  |

#### PageAtomPipeline
##### 分页数据包装模型

| 参数名称       | 参数类型                                | 必须  | 参数说明  |
| ---------- | ----------------------------------- | --- | ----- |
| count      | integer                             | √   | 总记录行数 |
| page       | integer                             | √   | 第几页   |
| pageSize   | integer                             | √   | 每页多少条 |
| records    | List<[AtomPipeline](#AtomPipeline)> | √   | 数据    |
| totalPages | integer                             | √   | 总共多少页 |

#### AtomPipeline
##### 流水线信息

| 参数名称         | 参数类型   | 必须  | 参数说明       |
| ------------ | ------ | --- | ---------- |
| atomVersion  | string | √   | 流水线使用的插件版本 |
| bgName       | string | √   | 所属BG       |
| centerName   | string | √   | 所属中心       |
| deptName     | string | √   | 所属部门       |
| pipelineId   | string | √   | 流水线ID      |
| pipelineName | string | √   | 流水线名称      |
| projectCode  | string | √   | 项目标识       |
| projectName  | string | √   | 所属项目       |

 
