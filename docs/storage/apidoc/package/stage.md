# Stage制品晋级接口

[toc]

## 制品晋级

- API: POST /repository/api/stage/upgrade/{projectId}/{repoName}/{packgekey}/{version}?tag=@release
- API 名称: upgrade_stage
- 功能说明：
  - 中文：制品晋级
  - English：upgrade stage
- 请求体
  此接口无请求体

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |packageKey|string|是|无|包唯一key|package unique key|
  |version|string|是|无|版本名称|version name|
  |tag|string|否|无|不填则默认晋级下一阶段，可选值:@prerelease、@release。前面的@可以省略|new stage tag|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId":  null
  }
  ```
