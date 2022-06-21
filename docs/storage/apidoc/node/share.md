# Share文件分享接口

[toc]

## 创建分享下载链接

- API: POST /repository/api/share/{projectId}/{repoName}/{fullPath}
- API 名称: create_share_url
- 功能说明：
  - 中文：创建分享下载链接
  - English：create share url
- 请求体

  ```json
  {
    "authorizedUserList": ["user1", "user2"],
    "authorizedIpList": ["192.168.1.1", "127.0.0.1"],
    "expireSeconds": 3600
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|
  |authorizedUserList|[string]|否|无|授权用户列表，若为空所有用户可下载|share user list|
  |authorizedIpList|[string]|否|无|授权ip列表，若为空所有ip可下载|share ip list|
  |expireSeconds|long|否|0|下载链接有效时间，单位秒|expire seconds|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": {
      "projectId": "test",
      "repoName": "generic-local",
      "fullPath": "/test.txt",
      "shareUrl": "/api/share/test/generic-local/test.json?token=bef56a14c33342beba7fdb5f63508d24",
      "authorizedUserList": [
        "user1",
        "user2"
      ],
      "authorizedIpList": [
        "192.168.1.1",
        "127.0.0.1"
      ],
      "expireDate": "2020-08-13T12:35:38.541"
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目id|project id|
  |repoName|string|仓库名称|repo name|
  |fullPath|string|完整路径|full path|
  |shareUrl|string|分享下载链接|share url|
  |authorizedUserList|list|授权用户列表|authorized user list|
  |authorizedIpList|list|授权ip列表|authorized ip list|
  |expireDate|string|过期时间|expire date|

## 创建分享下载链接（批量）

- API: POST /repository/api/share/batch
- API 名称: create_batch_share_url
- 功能说明：
  - 中文：创建分享下载链接（批量）
  - English：create batch share url
- 请求体

  ```json
  {
    "projectId": "",
    "repoName": "",
    "fullPathList": "",
    "authorizedUserList": ["user1", "user2"],
    "authorizedIpList": ["192.168.1.1", "127.0.0.1"],
    "expireSeconds": 3600
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPathList|string|是|无|完整路径列表|full path list|
  |authorizedUserList|[string]|否|无|授权用户列表，若为空所有用户可下载|share user list|
  |authorizedIpList|[string]|否|无|授权ip列表，若为空所有ip可下载|share ip list|
  |expireSeconds|long|否|0|下载链接有效时间，单位秒|expire seconds|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": {
      [
        "projectId": "test",
        "repoName": "generic-local",
        "fullPath": "/test.txt",
        "shareUrl": "/api/share/test/generic-local/test.json?token=bef56a14c33342beba7fdb5f63508d24",
        "authorizedUserList": [
          "user1",
          "user2"
        ],
        "authorizedIpList": [
          "192.168.1.1",
          "127.0.0.1"
        ],
        "expireDate": "2020-08-13T12:35:38.541"
      ]
    },
    "traceId": null
  }
  ```


- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目id|project id|
  |repoName|string|仓库名称|repo name|
  |fullPath|string|完整路径|full path|
  |shareUrl|string|分享下载链接|share url|
  |authorizedUserList|[string]|授权用户列表|authorized user list|
  |authorizedIpList|[string]|授权ip列表|authorized ip list|
  |expireDate|string|过期时间|expire date|

## 分享链接下载

- API: GET /repository/api/share/{projectId}/{repoName}/{fullPath}?token=xxx
- API 名称: download_share_url
- 功能说明：
  - 中文：分享链接下载，支持HEAD操作
  - English：download by share url
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|
  |token|string|是|无|下载凭证|download token|

- 请求头

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |Range|string|否|无|RFC 2616 中定义的字节范围，范围值必须使用 bytes=first-last 格式且仅支持单一范围，不支持多重范围。first 和 last 都是基于0开始的偏移量。例如 bytes=0-9，表示下载对象的开头10个字节的数据；bytes=5-9，表示下载对象的第6到第10个字节。此时返回 HTTP 状态码206（Partial Content）及 Content-Range 响应头部。如果 first 超过对象的大小，则返回 HTTP 状态码416（Requested Range Not Satisfiable）错误。如果不指定，则表示下载整个对象|bytes range|

- 响应头

  |字段|类型|说明|Description|
  |---|---|---|---|
  |Accept-Ranges|string|RFC 2616 中定义的服务器接收Range范围|RFC 2616 Accept-Ranges|
  |Cache-Control|string|RFC 2616 中定义的缓存指令|RFC 2616 Cache-Control|
  |Connection|string|RFC 2616 中定义，表明响应完成后是否会关闭网络连接。枚举值：keep-alive，close。|RFC 2616 Connection|
  |Content-Disposition|string|RFC 2616 中定义的文件名称|RFC 2616 Content-Disposition|
  |Content-Length|long|RFC 2616 中定义的 HTTP 响应内容长度（字节）|RFC 2616 Content Length|
  |Content-Range|string|RFC 2616 中定义的返回内容的字节范围，仅当请求中指定了 Range 请求头部时才会返回该头部|RFC 2616 Content-Range|
  |Content-Type|string|RFC 2616 中定义的 HTTP 响应内容类型（MIME）|RFC 2616 Content Length|
  |Date|string|RFC 1123 中定义的 GMT 格式服务端响应时间，例如Mon, 27 Jul 2020 08:51:59 GMT|RFC 1123 Content Length|
  |Etag|string|ETag 全称为 Entity Tag，是文件被创建时标识对象内容的信息标签，可用于检查对象的内容是否发生变化，通用制品文件会返回文件的sha256值|ETag, file sha256 checksum|
  |Last-Modified|string|文件的最近一次上传的时间，例如Mon, 27 Jul 2020 08:51:58 GMT|file last modified time|

- 响应体
  [文件流]
