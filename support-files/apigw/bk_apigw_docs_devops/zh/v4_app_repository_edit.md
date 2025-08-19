### 请求方法/请求路径
#### PUT /{apigwType}/v4/repositories/projects/{projectId}/repository
### 资源描述
#### 编辑关联代码库
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称             | 参数类型   | 必须  | 参数说明    |
| ---------------- | ------ | --- | ------- |
| repositoryHashId | String | √   | 代码库哈希ID |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称  | 参数类型                      | 必须   |
| ----- | ------------------------- | ---- |
| 代码库模型 | [Repository](#Repository) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X PUT '[请替换为上方API地址栏请求地址]?repositoryHashId={repositoryHashId}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### PUT 请求样例

```Json
{
  "@type" : "",
  "aliasName" : "",
  "credentialId" : "",
  "enablePac" : false,
  "externalId" : "",
  "formatURL" : "",
  "legal" : false,
  "projectId" : "",
  "projectName" : "",
  "repoHashId" : "",
  "scmCode" : "",
  "scmType" : "enum",
  "startPrefix" : "",
  "url" : "",
  "userName" : "",
  "yamlSyncStatus" : ""
}
```

### default 返回样例

```Json
{
  "data" : false,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### Repository
##### 代码库模型-多态基类

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明                                                                                                                                                                            |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| @type          | string                                                                                      | √   | 用于指定实现某一多态类, 可选[CodeSvnRepository, CodeGitRepository, CodeGitlabRepository, GithubRepository, CodeTGitRepository, CodeP4Repository, ScmGitRepository, ScmSvnRepository],具体实现见下方 |
| aliasName      | string                                                                                      |     |                                                                                                                                                                                 |
| credentialId   | string                                                                                      | √   |                                                                                                                                                                                 |
| enablePac      | boolean                                                                                     |     |                                                                                                                                                                                 |
| externalId     | string                                                                                      |     |                                                                                                                                                                                 |
| formatURL      | string                                                                                      |     |                                                                                                                                                                                 |
| legal          | boolean                                                                                     |     |                                                                                                                                                                                 |
| projectId      | string                                                                                      |     |                                                                                                                                                                                 |
| projectName    | string                                                                                      |     |                                                                                                                                                                                 |
| repoHashId     | string                                                                                      |     |                                                                                                                                                                                 |
| scmCode        | string                                                                                      |     |                                                                                                                                                                                 |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |                                                                                                                                                                                 |
| startPrefix    | string                                                                                      |     |                                                                                                                                                                                 |
| url            | string                                                                                      |     |                                                                                                                                                                                 |
| userName       | string                                                                                      |     |                                                                                                                                                                                 |
| yamlSyncStatus | string                                                                                      |     |                                                                                                                                                                                 |

#### CodeSvnRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [codeSvn] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明      |
| -------------- | ------------------------------------------------------------------------------------------- | --- | --------- |
| @type          | string                                                                                      | 必须是 | 多态类实现     | codeSvn |
| aliasName      | string                                                                                      | √   | 代码库别名     |
| credentialId   | string                                                                                      | √   | 凭据id      |
| credentialType | string                                                                                      |     | 凭证类型      |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac |
| externalId     | string                                                                                      |     |           |
| formatURL      | string                                                                                      |     |           |
| legal          | boolean                                                                                     |     |           |
| projectId      | string                                                                                      |     | 项目id      |
| projectName    | string                                                                                      | √   | svn项目名称   |
| region         | ENUM(TC, SH, BJ, GZ, CD, GROUP)                                                             |     | SVN区域     |
| repoHashId     | string                                                                                      |     | 仓库hash id |
| scmCode        | string                                                                                      | √   | 代码库标识     |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |           |
| startPrefix    | string                                                                                      |     |           |
| svnType        | string                                                                                      |     | SVN类型     |
| url            | string                                                                                      | √   | URL       |
| userName       | string                                                                                      | √   | 用户名       |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态  |

#### CodeGitRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [codeGit] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明                          |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ----------------------------- |
| @type          | string                                                                                      | 必须是 | 多态类实现                         | codeGit |
| aliasName      | string                                                                                      | √   | 代码库别名                         |
| atom           | boolean                                                                                     |     | 是否为插件库                        |
| authType       | ENUM(SSH, HTTP, HTTPS, OAUTH)                                                               |     | 仓库认证类型                        |
| credentialId   | string                                                                                      | √   | 凭据id(该凭证需要有git仓库Reporter以上权限) |
| credentialType | string                                                                                      |     | 凭证类型                          |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac                     |
| externalId     | string                                                                                      |     |                               |
| formatURL      | string                                                                                      |     |                               |
| gitProjectId   | integer                                                                                     |     | Git仓库ID                       |
| legal          | boolean                                                                                     |     |                               |
| projectId      | string                                                                                      |     | 项目id                          |
| projectName    | string                                                                                      | √   | git项目名称                       |
| repoHashId     | string                                                                                      |     | 仓库hash id                     |
| scmCode        | string                                                                                      | √   | 代码库标识                         |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |                               |
| startPrefix    | string                                                                                      |     |                               |
| url            | string                                                                                      | √   | URL                           |
| userName       | string                                                                                      | √   | 用户名                           |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态                      |

#### CodeGitlabRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [codeGitLab] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明       |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ---------- |
| @type          | string                                                                                      | 必须是 | 多态类实现      | codeGitLab |
| aliasName      | string                                                                                      | √   | 代码库别名      |
| authType       | ENUM(SSH, HTTP, HTTPS, OAUTH)                                                               |     | 仓库认证类型     |
| credentialId   | string                                                                                      | √   | 凭据id       |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac  |
| externalId     | string                                                                                      |     |            |
| formatURL      | string                                                                                      |     |            |
| gitProjectId   | integer                                                                                     |     | Gitlab仓库ID |
| legal          | boolean                                                                                     |     |            |
| projectId      | string                                                                                      |     | 项目id       |
| projectName    | string                                                                                      | √   | gitlab项目名称 |
| repoHashId     | string                                                                                      |     | 仓库hash id  |
| scmCode        | string                                                                                      | √   | 代码库标识      |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |            |
| startPrefix    | string                                                                                      |     |            |
| url            | string                                                                                      | √   | URL        |
| userName       | string                                                                                      | √   | 用户名        |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态   |

#### GithubRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [github] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明       |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ---------- |
| @type          | string                                                                                      | 必须是 | 多态类实现      | github |
| aliasName      | string                                                                                      | √   | 代码库别名      |
| credentialId   | string                                                                                      | √   |            |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac  |
| externalId     | string                                                                                      |     |            |
| formatURL      | string                                                                                      |     |            |
| gitProjectId   | integer                                                                                     |     | Git仓库ID    |
| legal          | boolean                                                                                     |     |            |
| projectId      | string                                                                                      | √   | 项目id       |
| projectName    | string                                                                                      | √   | github项目名称 |
| repoHashId     | string                                                                                      |     | 仓库hash id  |
| scmCode        | string                                                                                      | √   | 代码库标识      |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |            |
| startPrefix    | string                                                                                      |     |            |
| url            | string                                                                                      | √   | URL        |
| userName       | string                                                                                      | √   | 用户名        |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态   |

#### CodeTGitRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [codeTGit] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明      |
| -------------- | ------------------------------------------------------------------------------------------- | --- | --------- |
| @type          | string                                                                                      | 必须是 | 多态类实现     | codeTGit |
| aliasName      | string                                                                                      | √   | 代码库别名     |
| authType       | ENUM(SSH, HTTP, HTTPS, OAUTH)                                                               |     | 仓库认证类型    |
| credentialId   | string                                                                                      | √   | 凭据id      |
| credentialType | string                                                                                      |     | 凭证类型      |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac |
| externalId     | string                                                                                      |     |           |
| formatURL      | string                                                                                      |     |           |
| gitProjectId   | integer                                                                                     |     | TGit仓库ID  |
| legal          | boolean                                                                                     |     |           |
| projectId      | string                                                                                      |     | 项目id      |
| projectName    | string                                                                                      | √   | tGit项目名称  |
| repoHashId     | string                                                                                      |     |           |
| scmCode        | string                                                                                      | √   | 代码库标识     |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |           |
| startPrefix    | string                                                                                      |     |           |
| url            | string                                                                                      | √   | URL       |
| userName       | string                                                                                      | √   | 用户名       |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态  |

#### CodeP4Repository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [codeP4] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明               |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ------------------ |
| @type          | string                                                                                      | 必须是 | 多态类实现              | codeP4 |
| aliasName      | string                                                                                      | √   | 代码库别名              |
| credentialId   | string                                                                                      | √   | 凭据id               |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac          |
| externalId     | string                                                                                      |     |                    |
| formatURL      | string                                                                                      |     |                    |
| legal          | boolean                                                                                     |     |                    |
| projectId      | string                                                                                      |     | 项目id               |
| projectName    | string                                                                                      | √   | 项目名称(与aliasName相同) |
| repoHashId     | string                                                                                      |     | 仓库hash id          |
| scmCode        | string                                                                                      | √   | 代码库标识              |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |                    |
| startPrefix    | string                                                                                      |     |                    |
| url            | string                                                                                      | √   | URL                |
| userName       | string                                                                                      | √   | 用户名                |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态           |

#### ScmGitRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [scmGit] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明                          |
| -------------- | ------------------------------------------------------------------------------------------- | --- | ----------------------------- |
| @type          | string                                                                                      | 必须是 | 多态类实现                         | scmGit |
| aliasName      | string                                                                                      | √   | 代码库别名                         |
| atom           | boolean                                                                                     |     | 是否为插件库                        |
| authType       | ENUM(SSH, HTTP, HTTPS, OAUTH)                                                               |     | 仓库认证类型                        |
| credentialId   | string                                                                                      | √   | 凭据id(该凭证需要有git仓库Reporter以上权限) |
| credentialType | string                                                                                      |     | 凭证类型                          |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac                     |
| externalId     | string                                                                                      |     |                               |
| formatURL      | string                                                                                      |     |                               |
| gitProjectId   | integer                                                                                     |     | Git仓库ID                       |
| legal          | boolean                                                                                     |     |                               |
| projectId      | string                                                                                      |     | 项目id                          |
| projectName    | string                                                                                      | √   | git项目名称                       |
| repoHashId     | string                                                                                      |     | 仓库hash id                     |
| scmCode        | string                                                                                      | √   | 代码库标识                         |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |                               |
| startPrefix    | string                                                                                      |     |                               |
| url            | string                                                                                      | √   | URL                           |
| userName       | string                                                                                      | √   | 用户名                           |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态                      |

#### ScmSvnRepository
 *多态基类 <Repository> 的实现处, 其中当字段 @type = [scmSvn] 时指定为该类实现*
 

| 参数名称           | 参数类型                                                                                        | 必须  | 参数说明      |
| -------------- | ------------------------------------------------------------------------------------------- | --- | --------- |
| @type          | string                                                                                      | 必须是 | 多态类实现     | scmSvn |
| aliasName      | string                                                                                      | √   | 代码库别名     |
| credentialId   | string                                                                                      | √   | 凭据id      |
| credentialType | string                                                                                      |     | 凭证类型      |
| enablePac      | boolean                                                                                     |     | 仓库是否开启pac |
| externalId     | string                                                                                      |     |           |
| formatURL      | string                                                                                      |     |           |
| legal          | boolean                                                                                     |     |           |
| projectId      | string                                                                                      |     | 项目id      |
| projectName    | string                                                                                      | √   | svn项目名称   |
| region         | ENUM(TC, SH, BJ, GZ, CD, GROUP)                                                             |     | SVN区域     |
| repoHashId     | string                                                                                      |     | 仓库hash id |
| scmCode        | string                                                                                      | √   | 代码库标识     |
| scmType        | ENUM(CODE_SVN, CODE_GIT, CODE_GITLAB, GITHUB, CODE_TGIT, CODE_P4, SCM_GIT, SCM_SVN, SCM_P4) |     |           |
| startPrefix    | string                                                                                      |     |           |
| svnType        | string                                                                                      |     | SVN类型     |
| url            | string                                                                                      | √   | URL       |
| userName       | string                                                                                      | √   | 用户名       |
| yamlSyncStatus | string                                                                                      |     | yaml同步状态  |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
