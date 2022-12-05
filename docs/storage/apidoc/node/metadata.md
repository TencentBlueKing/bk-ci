# Metadata元数据接口

[toc]

## 查询元数据

- API: GET /repository/api/metadata/{projectId}/{repoName}/{fullPath}
- API 名称: query_metadata
- 功能说明：
  - 中文：查询元数据信息
  - English：query metadata info
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": {
      "key1": "value1",
      "key2": "value2"
    },
    "traceId": null
  }
  ```

- data字段说明

  键值对，key为元数据名称，value为元数据值

### 保存（更新）元数据

- API: POST /repository/api/metadata/{projectId}/{repoName}/{fullPath}
- API 名称: save_metadata
- 功能说明：
  - 中文：保存（更新）元数据信息，元数据不存在则保存，存在则更新
  - English：save metadata info
- 请求体

  ```json
  {
    "key1": "value1",
    "key2": "value2"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|
  |key|string|否|无|元数据键值对|metadata key-value|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

### 删除元数据

- API: DELETE /repository/api/metadata/{projectId}/{repoName}/{fullPath}
- API 名称: delete_metadata
- 功能说明：
  - 中文：根据提供的key列表删除元数据
  - English：delete metadata info
- 请求体

  ```json
  {
    "keyList": ["key1", "key2"]
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|
  |keyList|[string]|是|无|待删除的元数据key列表|metadata key list|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```
