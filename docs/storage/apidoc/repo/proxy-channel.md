# ProxyChannel代理源接口

[toc]

## 查询公有源列表

- API: GET /repository/api/proxy-channel/list/public/{repoType}
- API 名称: list_public_proxy_channel
- 功能说明：
  - 中文：列表查询公有源
  - English：list puiblic proxy channel
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |repoType|string|是|无|仓库类型，枚举值|repo type|

- 响应体

  ```json
  {
    "code" : 0,
    "message" : null,
    "data" : [ {
      "id" : "5f48b52fdf23460c0e2251e9",
      "public" : true,
      "name" : "maven-center",
      "url" : "http://http://center.maven.com",
      "repoType" : "MAVEN"
    } ],
    "traceId" : null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|代理源id|proxy channel id|
  |public|boolean|是否为公有源|is public channel|
  |name|string|代理源名称|repo name|
  |url|string|代理源url|repo category|
  |repoType|string|仓库类型|repo type|

