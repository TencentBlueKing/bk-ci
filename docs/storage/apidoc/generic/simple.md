# Generic通用制品仓库简单文件操作

[toc]

## 上传文件

- API: PUT /generic/{project}/{repo}/{path}
- API 名称: upload
- 功能说明：
	- 中文：上传通用制品文件
	- English：upload generic artifact file

- 请求体
[文件流]

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |project|string|是|无|项目名称|project name|
  |repo|string|是|无|仓库名称|repo name|
  |path|string|是|无|完整路径|full path|

- 请求头

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |X-BKREPO-SHA256|string|否|无|文件sha256|file sha256|
  |X-BKREPO-MD5|string|否|无|文件md5|file md5|
  |X-BKREPO-OVERWRITE|boolean|否|false|是否覆盖已存在文件|overwrite exist file|
  |X-BKREPO-EXPIRES|long|否|0|过期时间，单位天(0代表永久保存)|file expired days|
  |X-BKREPO-META-{key}|string|否|无|文件元数据，{key}表示元数据key，可以添加多个。key大小写不敏感，按小写存储|file metadata|
  |X-BKREPO-META|string|否|无|文件元数据，格式为base64(key1=value1&key2=value2)。key大小写敏感。当`X-BKREPO-META-{key}`不能满足需求时可用此字段代替|file metadata|


- 响应体

  ``` json
  {
    "code" : 0,
    "message" : null,
    "data" : {
      "createdBy" : "admin",
      "createdDate" : "2020-07-27T16:02:31.394",
      "lastModifiedBy" : "admin",
      "lastModifiedDate" : "2020-07-27T16:02:31.394",
      "folder" : false,
      "path" : "/",
      "name" : "test.json",
      "fullPath" : "/test.json",
      "size" : 34,
      "sha256" : "6a7983009447ecc725d2bb73a60b55d0ef5886884df0ffe3199f84b6df919895",
      "md5" : "2947b3932900d4534175d73964ec22ef",
      "metadata": {},
      "projectId" : "test",
      "repoName" : "generic-local"
    },
    "traceId" : null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
  |folder|bool|是否为文件夹|is folder|
  |path|string|节点目录|node path|
  |name|string|节点名称|node name|
  |fullPath|string|节点完整路径|node full path|
  |size|long|文件大小|file size|
  |sha256|string|文件sha256|file sha256|
  |md5|string|文件md5|file md5 checksum|
  |metadata|object|节点元数据|node metadata|
  |projectId|string|节点所属项目|node project id|
  |repoName|string|节点所属仓库|node repository name|

## 下载文件

- API: GET /generic/{project}/{repo}/{path}?download=true
- API 名称: download
- 功能说明：
  - 中文：下载通用制品文件
  - English：download generic file

- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |project|string|是|无|项目名称|project name|
  |repo|string|是|无|仓库名称|repo name|
  |path|string|是|无|完整路径|full path|
  |download|boolean|否|false|是否为下载请求。如果为true，响应体会添加Content-Disposition，强制浏览器进行下载；不加此参数，浏览器将根据情况展示文件预览 |is download request|

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
  |Content-Disposition|string|RFC 2616 中定义的文件名称，当download=true才会添加此参数|RFC 2616 Content-Disposition|
  |Content-Length|long|RFC 2616 中定义的 HTTP 响应内容长度（字节）|RFC 2616 Content Length|
  |Content-Range|string|RFC 2616 中定义的返回内容的字节范围，仅当请求中指定了 Range 请求头部时才会返回该头部|RFC 2616 Content-Range|
  |Content-Type|string|RFC 2616 中定义的 HTTP 响应内容类型（MIME）|RFC 2616 Content Length|
  |Date|string|RFC 1123 中定义的 GMT 格式服务端响应时间，例如Mon, 27 Jul 2020 08:51:59 GMT|RFC 1123 Content Length|
  |Etag|string|ETag 全称为 Entity Tag，是文件被创建时标识对象内容的信息标签，可用于检查对象的内容是否发生变化，通用制品文件会返回文件的sha256值|ETag, file sha256 checksum|
  |Last-Modified|string|文件的最近一次上传的时间，例如Mon, 27 Jul 2020 08:51:58 GMT|file last modified time|

- 响应体
  [文件流]

## 获取文件头部信息

- API: HEAD /generic/{project}/{repo}/{path}
- API 名称: head
- 功能说明：
  - 中文：获取通用制品文件头部信息
  - English：get generic file head info

- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |project|string|是|无|项目名称|project name|
  |repo|string|是|无|仓库名称|repo name|
  |path|string|是|无|完整路径|full path|

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
  此接口响应体为空

## 删除文件

- API: DELETE /generic/{project}/{repo}/{path}
- API 名称: delete
- 功能说明：
  - 中文：删除通用制品文件
  - English：delete generic file

- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |project|string|是|无|项目名称|project name|
  |repo|string|是|无|仓库名称|repo name|
  |path|string|是|无|完整路径|full path|

- 响应体

  ``` json
  {
    "code" : 0,
    "message" : null,
    "data" : null,
    "traceId" : null
  }
  ```

## 批量下载
- API : GET /generic/batch/{project}/{repo}
- API名称： batch download
- 功能说明：
   - 中文：批量下载通用制品文件
   - English：batch download generic file
- 请求body
``` json
{
  "paths": ["/dir/file1", "/file2"]
}
```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |project|string|是|无|项目名称|project name|
  |repo|string|是|无|仓库名称|repo name|
  |paths|list|是|无|文件完整路径，支持添加多个文件路径|full path list|


- 响应体
  [文件流]