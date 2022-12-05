# Generic通用制品仓库临时访问凭证接口

[toc]

## 创建临时访问token

- API: POST /generic/temporary/token/create
- API 名称: create_temporary_access_token
- 功能说明：
	- 中文：创建临时访问token
	- English：create temporary access token

- 请求体

  ``` json
  {
    "projectId": "test",
    "repoName": "generic-local",
    "fullPathSet": ["/path/file"],
    "authorizedUserSet": ["user1", "user2"],
    "authorizedIpSet": ["127.0.0.1", "192.168.191.1"],
    "expireSeconds": 3600,
    "permits": 1,
    "type": "DOWNLOAD"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPathSet|list|是|无|授权路径列表，支持批量创建|full path set|
  |authorizedUserSet|list|否|无|授权访问用户，不传则任意用户可访问|authorized user set|
  |authorizedIpSet|list|否|无|授权访问ip，不传则任意ip可访问|authorized ip set|
  |expireSeconds|long|否|3600*24|token有效时间，单位秒，小于等于0则永久有效|expire seconds|
  |permits|int|否|null|允许访问次数，null表示无限制|access permits|
  |type|string|是|无|token类型。UPLOAD: 运行上传, DOWNLOAD: 允许下载, ALL: 同时运行上传和下载|token type|

- 响应体

``` json
{
  "code" : 0,
  "message" : null,
  "data" : [{
    "projectId": "test",
    "repoName": "generic-local",
    "fullPath": "/path1",
    "token": "xxx",
    "authorizedUserSet": ["user1", "user2"],
    "authorizedIpSet": ["127.0.0.1", "192.168.191.1"],
    "expireDate": "2020-02-02T02:02:02.002",
    "permits": 1,
    "type": "DOWNLOAD"
  }],
  "traceId" : null
}
```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目名称|project name|
  |repoName|string|仓库名称|repo name|
  |fullPath|string|完整路径|node full path|
  |token|string|临时访问token|temporary access token|
  |authorizedUserSet|list|授权访问用户|authorized user set|
  |authorizedIpSet|list|授权访问ip|authorized ip set|
  |expireDate|string|过期时间，null表示永久token|expire date|
  |permits|int|允许访问次数，null表示无限制|access permits|
  |type|string|token类型|token type|

## 创建临时访问url

- API: POST /generic/temporary/url/create
- API 名称: create_temporary_access_url
- 功能说明：
	- 中文：创建临时访问url
	- English：create temporary access url

- 请求体

  ``` json
  {
    "projectId": "test",
    "repoName": "generic-local",
    "fullPathSet": ["/path/file"],
    "authorizedUserSet": ["user1", "user2"],
    "authorizedIpSet": ["127.0.0.1", "192.168.191.1"],
    "expireSeconds": 3600,
    "permits": 1,
    "type": "DOWNLOAD",
    "host": "http://custom-host.com/",
    "needsNotify": false
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPathSet|list|是|无|授权路径列表，支持批量创建|full path set|
  |authorizedUserSet|list|否|无|授权访问用户，不传则任意用户可访问|authorized user set|
  |authorizedIpSet|list|否|无|授权访问ip，不传则任意ip可访问|authorized ip set|
  |expireSeconds|long|否|3600*24|token有效时间，单位秒，小于等于0则永久有效|expire seconds|
  |permits|int|否|null|允许访问次数，null表示无限制|access permits|
  |type|string|是|无|token类型。UPLOAD:允许上传, DOWNLOAD: 允许下载, ALL: 同时允许上传和下载|token type|
  |host|string|否|无|自定义分享链接host，不指定则使用系统默认host|custom url host|
  |needsNotify|boolean|否|false|是否通知授权访问用户|notify authorized users|

- 响应体

``` json
{
  "code" : 0,
  "message" : null,
  "data" : [{
    "projectId": "test",
    "repoName": "generic-local",
    "fullPath": "/path/file",
    "url": "http://bkrepo.example.com/generic/temporary/test/generic-local/path/file?token=xxx",
    "authorizedUserSet": ["user1", "user2"],
    "authorizedIpSet": ["127.0.0.1", "192.168.191.1"],
    "expireDate": "2020-02-02T02:02:02.002",
    "permits": 1,
    "type": "DOWNLOAD"
  }],
  "traceId" : null
}
```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目名称|project name|
  |repoName|string|仓库名称|repo name|
  |fullPath|string|完整路径|node full path|
  |url|string|临时访问url|temporary access url|
  |authorizedUserSet|list|授权访问用户|authorized user set|
  |authorizedIpSet|list|授权访问ip|authorized ip set|
  |expireDate|string|过期时间，null表示永久token|expire date|
  |permits|int|允许访问次数，null表示无限制|access permits|
  |type|string|token类型|token type|

## 临时token文件下载接口

- API: GET /generic/temporary/download/{project}/{repo}/{path}?token=xxx
- API 名称: temporary download
- 功能说明：
	- 中文：临时token文件下载
	- English：temporary download
- 接口说明
  除`token`参数外，其余参数和协议和[generic下载文件](./simple.md)一致
  
## 临时token文件上传接口

- API: PUT /generic/temporary/upload/{project}/{repo}/{path}?token=xxx
- API 名称: temporary upload
- 功能说明：
	- 中文：临时token文件上传
	- English：temporary upload
- 接口说明
  除`token`参数外，其余参数和协议和[generic上传文件](./simple.md)一致
