# Repository仓库接口

[toc]

## 创建仓库

- API: POST /repository/api/repo/create
- API 名称: create_repo
- 功能说明：
  - 中文：创建仓库
  - English：create repo
- 请求体

  ```json
  {
    "projectId": "test",
    "name": "generic-local",
    "type": "GENERIC",
    "category": "LOCAL",
    "public": false,
    "description": "repo description",
    "configuration": null,
    "storageCredentialsKey": null,
    "quota": 1024
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |name|string|是|无|仓库名称|repo name|
  |type|string|是|无|仓库类型，枚举值|repo type|
  |category|string|否|COMPOSITE|仓库类别，枚举值|repo category|
  |public|boolean|否|false|是否公开|is public repo|
  |description|string|否|无|仓库描述|repo description|
  |configuration|object|否|无|仓库配置，参考后文|repo configuration|
  |storageCredentialsKey|string|否|无|存储凭证key|storage credentials key|
  |quota|long|否|无|仓库配额|repo quota|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```


## 更新仓库信息

- API: POST /repository/api/repo/update/{projectId}/{repoName}
- API 名称: update_repo
- 功能说明：
  - 中文：更新仓库信息
  - English：update repo
- 请求体
  ```json
  {
    "public": false,
    "description": "repo description",
    "configuration": null
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |public|boolean|否|无|是否公开。null则不修改|is public repo|
  |description|string|否|无|仓库描述。null则不修改|repo description|
  |configuration|RepositoryConfiguration|否|无|仓库配置，参考后文。null则不修改|repo configuration|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 删除仓库

- API: DELETE /repository/api/repo/delete/{projectId}/{repoName}?forced=false
- API 名称: delete_repo
- 功能说明：
  - 中文：删除仓库
  - English：delete repo

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |forced|boolean|否|false|是否强制删除。如果为false，当仓库中存在文件时，将无法删除仓库|force to delete repo|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 查询仓库信息

- API: GET /repository/api/repo/info/{projectId}/{repoName}/{type}
- API 名称: get_repo_info
- 功能说明：
  - 中文：查询仓库详情
  - English：get repo info
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |type|string|否|无|仓库类型|repo type|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "projectId" : "test",
      "name" : "local",
      "type" : "GENERIC",
      "category" : "LOCAL",
      "public" : false,
      "description" : "",
      "configuration": {},
      "createdBy" : "system",
      "createdDate" : "2020-03-16T12:13:03.371",
      "lastModifiedBy" : "system",
      "lastModifiedDate" : "2020-03-16T12:13:03.371",
      "quota": 1024,
      "used": 100
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目id|project id|
  |name|string|仓库名称|repo name|
  |type|string|仓库类型|repo type|
  |category|string|仓库类别|repo category|
  |public|boolean|是否公开项目|is public repo|
  |description|string|仓库描述|repo description|
  |configuration|[object]|仓库配置，参考仓库配置介绍|repo configuration|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
  |quota|long|仓库配额，单位字节，值为nul时表示未设置仓库配额|repo quota|
  |used|long|仓库已使用容量，单位字节|repo used volume|

## 校验仓库是否存在

- API: GET /repository/api/repo/exist/{projectId}/{repoName}
- API 名称: check_repo_exist
- 功能说明：
  - 中文：校验仓库是否存在
  - English：check repo exist
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": true,
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |data|boolean|仓库是否存在|repo exist or not|

## 分页查询仓库

- API: GET /repository/api/repo/page/{projectId}/{pageNumber}/{pageSize}?name=local&type=GENERIC
- API 名称: list_repo_page
- 功能说明：
  - 中文：分页查询仓库
  - English：list repo page
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |pageNumber|int|是|无|当前页|page number|
  |pageSize|int|是|无|分页数量|page size|
  |name|string|否|无|仓库名称，支持前缀模糊匹配|repo name|
  |type|string|否|无|仓库类型，枚举值|repo type|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "pageNumber": 0,
      "pageSize": 1,
      "totalRecords": 18,
      "totalPages": 2,
      "records": [
        {
          "projectId" : "test",
          "name" : "local",
          "type" : "GENERIC",
          "category" : "LOCAL",
          "public" : false,
          "description" : "",
          "createdBy" : "system",
          "createdDate" : "2020-03-16T12:13:03.371",
          "lastModifiedBy" : "system",
          "lastModifiedDate" : "2020-03-16T12:13:03.371",
          "quota": 1024,
          "used": 100
        }
      ]
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目id|project id|
  |name|string|仓库名称|repo name|
  |type|string|仓库类型|repo type|
  |category|string|仓库类别|repo category|
  |public|boolean|是否公开项目|is public repo|
  |description|string|仓库描述|repo description|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
  |quota|long|仓库配额，单位字节，值为nul时表示未设置仓库配额|repo quota|
  |used|long|仓库已使用容量，单位字节|repo used volume|

## 列表查询仓库

- API: GET /repository/api/repo/list/{projectId}?name=local&type=GENERIC
- API 名称: list_repo
- 功能说明：
  - 中文：列表查询仓库
  - English：list repo
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |name|string|否|无|仓库名称，支持前缀模糊匹配|repo name|
  |type|string|否|无|仓库类型，枚举值|repo type|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
      {
        "projectId" : "test",
        "name" : "local",
        "type" : "GENERIC",
        "category" : "LOCAL",
        "public" : false,
        "description" : "",
        "createdBy" : "system",
        "createdDate" : "2020-03-16T12:13:03.371",
        "lastModifiedBy" : "system",
        "lastModifiedDate" : "2020-03-16T12:13:03.371",
        "quota": 1024,
        "used": 100
      }
    ],
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目id|project id|
  |name|string|仓库名称|repo name|
  |type|string|仓库类型|repo type|
  |category|string|仓库类别|repo category|
  |public|boolean|是否公开项目|is public repo|
  |description|string|仓库描述|repo description|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
  |quota|long|仓库配额，单位字节，值为nul时表示未设置仓库配额|repo quota|
  |used|long|仓库已使用容量，单位字节|repo used volume|

## 查询仓库配额

- API： GET /repository/api/repo/quota/{projectId}/{repoName}

- API 名称：get_repo_quota

- 功能说明：

  - 中文：查询仓库配额
  - English：get repo quota

- 请求体

  - 此接口无请求体

  - 请求字段说明

    | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description  |
    | --------- | ------ | -------- | ------ | -------- | ------------ |
    | projectId | string | 是       | 无     | 项目名称 | project name |
    | repoName  | string | 是       | 无     | 仓库名称 | repo name    |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "quota": 1024,
          "used": 100
      },
      "traceId": ""
  }
  ```

- data字段说明

  | 字段  | 类型 | 说明                                            | Description      |
  | ----- | ---- | ----------------------------------------------- | ---------------- |
  | quota | long | 仓库配额，单位字节，值为nul时表示未设置仓库配额 | repo quota       |
  | used  | long | 仓库已使用容量，单位字节                        | repo used volume |

## 修改仓库配额

- API： POST /repository/api/repo/quota/{projectId}/{repoName}

- API 名称：update_repo_quota

- 功能说明：

  - 中文：修改仓库配额
  - English：update repo quota

- 请求体

  ```
  quota=102400
  ```

  - 请求字段说明

    | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description  |
    | --------- | ------ | -------- | ------ | -------- | ------------ |
    | projectId | string | 是       | 无     | 项目名称 | project name |
    | repoName  | string | 是       | 无     | 仓库名称 | repo name    |
    | quota     | long   | 是       | 无     | 仓库配额 | repo quota   |

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

  - 此接口无返回data

## 仓库公共枚举值说明
### 仓库类型RepositoryType

> 用于标识仓库功能类型

|枚举值|说明|
|---|---|
|GENERIC|通用二进制文件仓库|
|DOCKER|Docker仓库|
|MAVEN|Maven仓库|
|PYPI|Pypi仓库|
|NPM|Npm仓库|
|HELM|Helm仓库|
|COMPOSER|Composer仓库|
|RPM|Rpm仓库|

### 仓库类别RepositoryCategory

> 用于标识仓库类别

|枚举值|说明|
|---|---|
|LOCAL|本地仓库。普通仓库，上传/下载构件都在本地进行。|
|REMOTE|远程仓库。通过访问远程地址拉取构件，不支持上传|
|VIRTUAL|虚拟仓库。可以组合多个本地仓库和远程仓库拉取构件，不支持上传|
|COMPOSITE|组合仓库。具有LOCAL的功能，同时也支持代理多个远程地址进行下载|

## RepositoryConfiguration仓库配置项
### 公共配置项

每一类配置都具有下列公共配置项

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|type|string|是|无|不同类型仓库分别对应local、remote、virtual、composite(小写)，用于反序列化，创建和修改时需要提供该字段|configuration type|
|settings|map|否|无|不同类型仓库可以通过该字段进行差异化配置|repo settings|

### local本地仓库配置项

|字段|类型|是否必须|默认值|说明|Description|
|:------|---|---|---|---|---|
|webHook|WebHook|否|无|WebHook相关配置|web hook|

- **WebHook配置项**

|字段|类型|是否必须|默认值|说明|Description|
|:----------|---|---|---|---|---|
|webHookList|[WebHookSetting]|否|无|WebHook 列表|web hook list|

- **WebHookSetting配置项**

|字段|类型|是否必须|默认值|说明|Description|
|:------|---|---|---|---|---|
|url|string|否|无|远程url地址|remote web hook url|
|headers|map|否|无|发起远程url的自定义headers|web hook headers|

### remote远程仓库配置项

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|url|string|否|空|远程地址|remote repository url|
|credentials|RemoteCredentialsConfiguration|否|默认配置|访问凭证配置|remote credentials configuration|
|network|RemoteNetworkConfiguration|否|默认配置|网络配置|remote network configuration|
|cache|RemoteCacheConfiguration|否|默认配置|缓存配置|remote cache configuration|

- **RemoteCredentialsConfiguration**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|username|string|否|无|远程仓库 用户名|remote repo username|
|password|string|否|无|远程仓库 密码|remote repo password|

- **RemoteNetworkConfiguration**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|proxy|NetworkProxyConfiguration|否|无|网络代理配置|network proxy|
|connectTimeout|long|否|10 * 1000|网络连接超时时间(单位ms)|network connect timeout|
|readTimeout|long|否|10 * 1000|网络读取超时时间(单位ms)|network read timeout|

- **NetworkProxyConfiguration**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|host|string|是|无|网络代理主机|proxy host|
|port|int|是|无|网络代理端口|proxy int|
|username|string|否|无|网络代理用户名|proxy username|
|password|string|否|无|网络代理密码|proxy password|

- **RemoteCacheConfiguration**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|enabled|boolean|否|true|是否开启缓存|cache enabled|
|expiration|long|否|---1|构件缓存过期时间（单位分钟，0或负数表示永久缓存）|cache expiration|

### virtual虚拟仓库配置项

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|repositoryList|[RepositoryIdentify]|否|无|仓库列表|repo list|

- **RepositoryIdentify**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|代理项目名称|project id|
|name|string|是|无|代理仓库名称|repo name|

### composite组合仓库配置项

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|proxy|ProxyConfiguration|否|无|仓库代理配置|repo proxy configuration|

- **ProxyConfiguration**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|channelList|[ProxyChannelSetting]|否|无|代理源列表|proxy channel list|

- **ProxyChannelSetting**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|public|boolean|是|无|是否为公有源|is public|
|name|string|否|无|代理源名称|proxy channel name|
|url|string|否|无|代理源地址|proxy channel url|
|credentialKey|string|否|无|鉴权凭据key|proxy credentials id|
|username|string|否|无|代理源认证用户名|channel username|
|password|string|否|无|代理源认证密码|channel password|

### 依赖源的差异化配置项

各个依赖源的差异化配置通过`settings`进行配置，每项配置的具体含义请参考依赖源文档。 