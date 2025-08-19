### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/repositoryCommit/commit_data_list
### 资源描述
#### 获取代码提交记录
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型   | 必须  | 参数说明    |
| ---------- | ------ | --- | ------- |
| buildId    | String | √   | buildID |
| pipelineId | String |     | 流水线ID   |

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
| default | [ResultListCommitResponse](#ResultListCommitResponse) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?buildId={buildId}&pipelineId={pipelineId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ {
    "elementId" : "",
    "name" : "",
    "records" : [ {
      "buildId" : "",
      "comment" : "",
      "commit" : "",
      "commitTime" : 0,
      "committer" : "",
      "elementId" : "",
      "pipelineId" : "",
      "repoId" : "",
      "repoName" : "",
      "repoUrl" : "",
      "type" : 0,
      "url" : ""
    } ]
  } ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListCommitResponse
##### 数据返回包装模型

| 参数名称    | 参数类型                                    | 必须  | 参数说明 |
| ------- | --------------------------------------- | --- | ---- |
| data    | List<[CommitResponse](#CommitResponse)> |     | 数据   |
| message | string                                  |     | 错误信息 |
| status  | integer                                 | √   | 状态码  |

#### CommitResponse
##### 提交返回模型

| 参数名称      | 参数类型                            | 必须  | 参数说明 |
| --------- | ------------------------------- | --- | ---- |
| elementId | string                          | √   | 插件ID |
| name      | string                          | √   | 仓库名称 |
| records   | List<[CommitData](#CommitData)> | √   | 记录   |

#### CommitData
##### 记录

| 参数名称       | 参数类型    | 必须  | 参数说明 |
| ---------- | ------- | --- | ---- |
| buildId    | string  |     |      |
| comment    | string  |     |      |
| commit     | string  |     |      |
| commitTime | integer |     |      |
| committer  | string  |     |      |
| elementId  | string  |     |      |
| pipelineId | string  |     |      |
| repoId     | string  |     |      |
| repoName   | string  |     |      |
| repoUrl    | string  |     |      |
| type       | integer |     |      |
| url        | string  |     |      |

 
