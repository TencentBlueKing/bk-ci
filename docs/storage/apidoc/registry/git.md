## GIT节点接口说明

### 同步仓库
- API: POST /{projectId}/{repoName}/sync?hub_type={hub_type}&owner={owner}

- API 名称: repository_sync

- 功能说明：

  - 中文：同步仓库
  - English：sync repository

- 请求体
    此接口请求体为空

- 请求字段说明

  | 字段                  | 类型    | 是否必须 | 默认值    | 说明                  | Description             |
  | --------------------- | ------- | -------- | --------- | --------------------- | ----------------------- |
  | projectId             | string  | 是       | 无        | 项目名称              | project name            |
  | repoName                  | string  | 是       | 无        | 仓库名称              | repo name               |
  | hub_type | enum | 否 | 无 | 仓库类型[github] | hub type[github] |
  | owner | string | 否 | 无 | 仓库拥有者，hub_type传的情况必须传 | hub owner,required when hub_type have a value |


- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": ""
  }
  ```

- data字段说明

  请求成功无返回数据
  
### 查询文件内容
- API: POST /{projectId}/{repoName}/raw/{ref}/{path}?hub_type={hub_type}&owner={owner}

- API 名称: get_file_content

- 功能说明：

  - 中文：查询文件内容
  - English：get file content

- 请求体
    此接口请求体为空

- 请求字段说明

  | 字段                  | 类型    | 是否必须 | 默认值    | 说明                  | Description             |
  | --------------------- | ------- | -------- | --------- | --------------------- | ----------------------- |
  | projectId             | string  | 是       | 无        | 项目名称              | project name            |
  | repoName             | string  | 是       | 无        | 仓库名称              | repo name               |
  | ref                  | string  | 是       | 无        | branch/tag/commit的名字， 默认分支为master  | The name of branch/tag/commit, default branch is master|
  | path                  | string  | 是       | 无        | 文件路径              | file path               |
  | hub_type | enum | 否 | 无 | 仓库类型[github] | hub type[github] |
  | owner | string | 否 | 无 | 仓库拥有者，hub_type传的情况必须传 | hub owner,required when hub_type have a value |


- 响应体
    [文件流]