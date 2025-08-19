### 请求方法/请求路径
#### GET /{apigwType}/v4/repositories/projects/{projectId}/repository_info_list
### 资源描述
#### 代码库列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明 |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ---- |
| repositoryType | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     | 仓库类型 |

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
| default | [ResultPageRepositoryInfo](#ResultPageRepositoryInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?repositoryType={repositoryType}' \
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
      "aliasName" : "",
      "createUser" : "",
      "createdTime" : 0,
      "remoteRepoId" : 0,
      "repositoryHashId" : "",
      "repositoryId" : 0,
      "type" : "enum",
      "updatedTime" : 0,
      "url" : ""
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageRepositoryInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                                      | 必须  | 参数说明 |
| ------- | ----------------------------------------- | --- | ---- |
| data    | [PageRepositoryInfo](#PageRepositoryInfo) |     |      |
| message | string                                    |     | 错误信息 |
| status  | integer                                   | √   | 状态码  |

#### PageRepositoryInfo
##### 分页数据包装模型

| 参数名称       | 参数类型                                    | 必须  | 参数说明  |
| ---------- | --------------------------------------- | --- | ----- |
| count      | integer                                 | √   | 总记录行数 |
| page       | integer                                 | √   | 第几页   |
| pageSize   | integer                                 | √   | 每页多少条 |
| records    | List<[RepositoryInfo](#RepositoryInfo)> | √   | 数据    |
| totalPages | integer                                 | √   | 总共多少页 |

#### RepositoryInfo
##### 代码库模型-基本信息

| 参数名称             | 参数类型                                                                                        | 必须  | 参数说明   |
| ---------------- | ------------------------------------------------------------------------------------------- | --- | ------ |
| aliasName        | string                                                                                      | √   | 仓库别名   |
| createUser       | string                                                                                      |     | 创建人    |
| createdTime      | integer                                                                                     |     | 创建时间   |
| remoteRepoId     | integer                                                                                     |     | 远程仓库ID |
| repositoryHashId | string                                                                                      |     | 仓库哈希ID |
| repositoryId     | integer                                                                                     |     | 仓库ID   |
| type             | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) | √   | 类型     |
| updatedTime      | integer                                                                                     | √   | 最后更新时间 |
| url              | string                                                                                      | √   | URL    |

 
