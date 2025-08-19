### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/artifactories/file_info
### 资源描述
#### 根据元数据获取文件(注意: 如果需要构建产物的下载url，请单独调用下载接口，如 v4_app_artifactory_userDownloadUrl)
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明            |
| ---------- | ------- | --- | --------------- |
| buildId    | String  | √   | 构建ID            |
| page       | integer |     | 第几页             |
| pageSize   | integer |     | 每页多少条(不传默认全部返回) |
| pipelineId | String  |     | 流水线ID           |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                      | 说明               |
| ------- | ----------------------------------------- | ---------------- |
| default | [ResultPageFileInfo](#ResultPageFileInfo) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?buildId={buildId}&page={page}&pageSize={pageSize}&pipelineId={pipelineId}' \
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
      "appVersion" : "",
      "artifactoryType" : "enum",
      "downloadUrl" : "",
      "folder" : false,
      "fullName" : "",
      "fullPath" : "",
      "md5" : "",
      "modifiedTime" : 0,
      "name" : "",
      "path" : "",
      "properties" : [ {
        "key" : "",
        "value" : ""
      } ],
      "registry" : "",
      "shortUrl" : "",
      "size" : 0
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageFileInfo
##### 数据返回包装模型

| 参数名称    | 参数类型                          | 必须  | 参数说明 |
| ------- | ----------------------------- | --- | ---- |
| data    | [PageFileInfo](#PageFileInfo) |     |      |
| message | string                        |     | 错误信息 |
| status  | integer                       | √   | 状态码  |

#### PageFileInfo
##### 分页数据包装模型

| 参数名称       | 参数类型                        | 必须  | 参数说明  |
| ---------- | --------------------------- | --- | ----- |
| count      | integer                     | √   | 总记录行数 |
| page       | integer                     | √   | 第几页   |
| pageSize   | integer                     | √   | 每页多少条 |
| records    | List<[FileInfo](#FileInfo)> | √   | 数据    |
| totalPages | integer                     | √   | 总共多少页 |

#### FileInfo
##### 版本仓库-文件信息

| 参数名称            | 参数类型                                      | 必须  | 参数说明            |
| --------------- | ----------------------------------------- | --- | --------------- |
| appVersion      | string                                    |     | app版本           |
| artifactoryType | ENUM(PIPELINE, CUSTOM_DIR, IMAGE, REPORT) | √   | 仓库类型            |
| downloadUrl     | string                                    |     | 下载链接            |
| folder          | boolean                                   | √   | 是否文件夹           |
| fullName        | string                                    | √   | 文件全名            |
| fullPath        | string                                    | √   | 文件全路径           |
| md5             | string                                    |     | MD5             |
| modifiedTime    | integer                                   | √   | 更新时间            |
| name            | string                                    | √   | 文件名             |
| path            | string                                    | √   | 文件路径            |
| properties      | List<[Property](#Property)>               |     | 元数据             |
| registry        | string                                    |     | docker registry |
| shortUrl        | string                                    |     | 下载短链接           |
| size            | integer                                   | √   | 文件大小(byte)      |

#### Property
##### 版本仓库-元数据

| 参数名称  | 参数类型   | 必须  | 参数说明 |
| ----- | ------ | --- | ---- |
| key   | string | √   | 元数据键 |
| value | string | √   | 元数据值 |

 
