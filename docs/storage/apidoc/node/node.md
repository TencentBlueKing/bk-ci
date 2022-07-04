# Node节点操作接口

[toc]

## 查询节点详情

- API: GET /repository/api/node/detail/{projectId}/{repoName}/{fullPath}
- API 名称: query_node_detail
- 功能说明：
  - 中文：查询节点详情
  - English：query node detail
- 请求体
  此接口请求体为空
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "projectId" : "test",
      "repoName" : "generic-local",
      "path" : "/",
      "name" : "test.json",
      "fullPath" : "/test.json",
      "folder" : false,
      "size" : 34,
      "sha256" : "6a7983009447ecc725d2bb73a60b55d0ef5886884df0ffe3199f84b6df919895",
      "md5" : "2947b3932900d4534175d73964ec22ef",
      "stageTag": "@release",
      "metadata": {
        "key": "value"
      },
      "createdBy" : "admin",
      "createdDate" : "2020-07-27T16:02:31.394",
      "lastModifiedBy" : "admin",
      "lastModifiedDate" : "2020-07-27T16:02:31.394"
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|节点所属项目|node project id|
  |repoName|string|节点所属仓库|node repository name|
  |path|string|节点目录|node path|
  |name|string|节点名称|node name|
  |fullPath|string|节点完整路径|node full path|
  |folder|bool|是否为文件夹|is folder|
  |size|long|节点大小|file size|
  |sha256|string|节点sha256|file sha256|
  |md5|string|节点md5|file md5 checksum|
  |stageTag|string|晋级状态标签|stage status tag|
  |metadata|object|节点元数据，key-value键值对|node metadata|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|

## 分页查询节点

- API: GET /repository/api/node/page/{projectId}/{repoName}/{fullPath}?pageNumber=0&pageSize=20&includeFolder=true&includeMetadata=true&deep=false&sort=false&sortProperty=xx&direction=ASC&sortProperty=xx&direction=DESC
- API 名称: list_node_page
- 功能说明：
  - 中文：分页查询节点，返回的结果列表中目录在前，文件在后，并按照文件名称排序, 同时支持自定义排序
  - English：list node page
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值| 说明                                                            |Description|
  |---|---|---|---------------------------------------------------------------|---|---|
  |projectId|string|是|无| 项目名称                                                          |project name|
  |repoName|string|是|无| 仓库名称                                                          |repo name|
  |fullPath|string|是|无| 完整路径                                                          |full path|
  |pageNumber|int|否|1| 当前页                                                           |current page|
  |pageSize|int|否|20| 分页大小                                                          |page size|
  |includeFolder|boolean|否|true| 是否包含目录                                                        |include folder|
  |includeMetadata|boolean|否|false| 是否包含元数据                                                       |include  metadata|
  |deep|boolean|否|false| 是否查询子目录节点                                                     |deep query|
  |sort|boolean|否|false| 是否排序输出结果                                                      |sort result|
  |sortProperty|string|否|false| 自定义排序字段,可添加多个。自定义排序字段优先级高于sort=true时的排序规则。多个自定义排序字段优先级与参数顺序相同 |sort property|
  |direction|string|否|false| 自定义排序方向,可添加多个，与排序字段一一对应                                       |sort direction|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "pageNumber": 1,
      "pageSize": 20,
      "totalRecords": 18,
      "totalPages": 18,
      "records": [
        {
          "projectId" : "test",
          "repoName" : "generic-local",
          "path" : "/",
          "name" : "test.json",
          "fullPath" : "/test.json",
          "folder" : false,
          "size" : 34,
          "sha256" : "6a7983009447ecc725d2bb73a60b55d0ef5886884df0ffe3199f84b6df919895",
          "md5" : "2947b3932900d4534175d73964ec22ef",
          "stageTag": "@release",
          "metadata": {},
          "createdBy" : "admin",
          "createdDate" : "2020-07-27T16:02:31.394",
          "lastModifiedBy" : "admin",
          "lastModifiedDate" : "2020-07-27T16:02:31.394"
        }
      ]
    },
    "traceId": null
  }
  ```

- record字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|节点所属项目|node project id|
  |repoName|string|节点所属仓库|node repository name|
  |path|string|节点目录|node path|
  |name|string|节点名称|node name|
  |fullPath|string|节点完整路径|node full path|
  |folder|bool|是否为文件夹|is folder|
  |size|long|节点大小|file size|
  |sha256|string|节点sha256|file sha256|
  |md5|string|节点md5|file md5 checksum|
  |stageTag|string|晋级状态标签，includeMetadata=false将返回空|stage status tag|
  |metadata|object|节点元数据，key-value键值对，includeMetadata=false将返回空|node metadata|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|

## 创建目录

- API: POST /repository/api/node/mkdir/{projectId}/{repoName}/{path}
- API 名称: mkdir
- 功能说明：
  - 中文：创建目录节点
  - English：create directory node
- 请求体
  此接口请求体为空
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |path|string|是|无|完整路径|full path|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 删除节点

- API: DELETE /repository/api/node/delete/{projectId}/{repoName}/{fullPath}
- API 名称: delete_node
- 功能说明：
  - 中文：删除节点，同时支持删除目录和文件节点
  - English：delete node
- 请求体
  此接口请求体为空
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 更新节点

- API: POST /repository/api/node/update/{projectId}/{repoName}/{fullPath}
- API 名称: update_node
- 功能说明：
  - 中文：更新节点信息，目前支持修改文件过期时间
  - English：update node info
- 请求体

  ```json
  {
    "expires": 0
  }
  ```

- 请求字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目名称|project name|
|repoName|string|是|无|仓库名称|repo name|
|fullPath|string|是|无|完整路径|full path|
|expires|long|否|0|过期时间，单位天(0代表永久保存)|expires day|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 重命名节点

- API: POST /repository/api/node/rename/{projectId}/{repoName}/{fullPath}?newFullPath=/data/new_name.text
- API 名称: rename_node
- 功能说明：
  - 中文：重命名节点
  - English：rename node
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|
  |newFullPath|string|是|无|新的完整路径|new full path|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 移动节点

- API: POST /repository/api/node/move
- API 名称: move_node
- 功能说明：
  - 中文：移动节点
  - English：move node

  移动文件或者文件夹，采用fast-failed模式，移动过程中出现错误则立即返回错误，剩下的文件不会再移动。该接口行为类似linux mv命令:

  - mv 文件 文件  -> 将源文件改名为目标文件
  - mv 文件 目录  -> 将源文件移动到目标目录
  - mv 目录 目录  -> 如果目标目录已存在，将源目录（目录本身及子文件）移动到目标目录；目标目录不存在则改名
  - mv 目录 文件  -> 出错

- 请求体

  ``` json
  {
    "srcProjectId": "",
    "srcRepoName": "",
    "srcFullPath": "",
    "destProjectId": "",
    "destRepoName": "",
    "destFullPath": "",
    "overwrite": false
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |srcProjectId|string|是|无|源项目名称|src project name|
  |srcRepoName|string|是|无|源仓库名称|src repo name|
  |srcFullPath|string|是|无|源完整路径|src full path|
  |destProjectId|string|否|null|目的项目名称。传null表示源项目|dest project name|
  |destRepoName|string|否|null|目的仓库名称。传null表示源仓库|dest repo name|
  |destFullPath|string|是|无|目的完整路径|dest full path|
  |overwrite|boolean|否|false|同名文件是否覆盖|overwrite  same node|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 拷贝节点

- API: POST /repository/api/node/copy
- API 名称: copy_node
- 功能说明：
  - 中文：拷贝节点
  - English：copy node

  拷贝文件或者文件夹，采用fast-failed模式，拷贝过程中出现错误则立即返回错误，剩下的文件不会再拷贝。该接口行为类似linux cp命令:

  - cp 文件 文件  -> 将源文件拷贝到目标文件
  - cp 文件 目录  -> 将文件移动到目标目录下
  - cp 目录 目录  -> 如果目标目录已存在，将源目录（目录本身及子文件）拷贝到目标目录；否则将源目录下文件拷贝到目标目录
  - cp 目录 文件  -> 出错

- 请求体

  ``` json
  {
    "srcProjectId": "",
    "srcRepoName": "",
    "srcFullPath": "",
    "destProjectId": "",
    "destRepoName": "",
    "destFullPath": "",
    "overwrite": false
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |srcProjectId|string|是|无|源项目名称|src project name|
  |srcRepoName|string|是|无|源仓库名称|src repo name|
  |srcFullPath|string|是|无|源完整路径|src full path|
  |destProjectId|string|否|null|目的项目名称。传null表示源项目|dest project name|
  |destRepoName|string|否|null|目的仓库名称。传null表示源仓库|dest repo name|
  |destFullPath|string|是|无|目的完整路径|dest full path|
  |overwrite|boolean|否|false|同名文件是否覆盖|overwrite  same node|

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 统计节点大小信息

- API: GET /repository/api/node/size/{projectId}/{repoName}/{fullPath}
- API 名称: compute_node_size
- 功能说明：
  - 中文：统计节点大小信息
  - English：compute node size
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|完整路径|full path|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "subNodeCount": 32,
      "size": 443022203
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |subNodeCount|long|子节点数量（包括目录）|sub node count|
  |size|long|目录/文件大小|directory/file size|

## 查询节点删除点

- API: GET /repository/api/node/list-deleted/{projectId}/{repoName}/{fullPath}
- API 名称: list_node_deleted
- 功能说明：
  - 中文：查询节点删除点
  - English：list node deleted point
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|删除的节点路径，可以为目录，也可以为文件|full path|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
        {
            "id": 1632451573064,
            "fullPath": "/test.txt",
            "size": 6,
            "sha256": "a73ec2f6ed66564f5714ed3d02aa6b54a70f47f0ecc5348c72fbc804a4b5f61c",
            "metadata": {},
            "deletedBy": "system",
            "deletedTime": "2021-09-24T10:46:13.064"
        }
    ],
    "traceId": ""
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|long|删除点id|delete point id|
  |fullPath|string|节点完整路径|node full path|
  |size|long|节点大小|file size|
  |sha256|string|节点sha256|file sha256|
  |metadata|object|节点元数据，key-value键值对|node metadata|
  |deletedBy|string|删除人|delete user|
  |deletedTime|string|删除时间|delete time|

## 恢复被删除节点

- API: POST /repository/api/node/restore/{projectId}/{repoName}/{fullPath}?deletedId=1632451573064&conflictStrategy=SKIP
- API 名称: restore_node
- 功能说明：
  - 中文：恢复被删除节点
  - English：restore deleted node
- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |fullPath|string|是|无|删除的节点路径，可以为目录，也可以为文件|full path|
  |deletedId|long|是|无|删除点id，通过查询获得|deleted point id|
  |conflictStrategy|string|是|SKIP|冲突处理策略。假如用户新创建了新的同名文件的冲突处理策略|conflict strategy|

- ConflictStrategy 冲突处理策略选项
  - SKIP: 跳过    
  - OVERWRITE: 覆盖
  - FAILED: 失败

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
        "fullPath": "/test.txt",
        "restoreCount": 1,
        "skipCount": 0,
        "conflictCount": 0
    },
    "traceId": ""
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |fullPath|string|节点完整路径|full path|
  |restoreCount|long|恢复数量|restore count|
  |skipCount|long|跳过数量|skip count|
  |conflictCount|long|冲突数量|conflict count|

## 自定义搜索

- API: POST /repository/api/node/search
- API 名称: node search
- 功能说明：
  - 中文：节点自定义搜索。最外层的查询条件中必须包含projectId条件，可以传入repoType指定仓库类型或者repoName指定仓库查询。
  - English：search node
- 请求体
  参考[自定义搜索接口公共说明](../common/search.md?id=自定义搜索协议)
  
  假设需要查询*项目test下, 仓库为generic-local1或generic-local2，文件名以.tgz结尾的文件，并按照文件名和大小排序，查询结果包含name、fullPath、size、sha256、md5、metadata字段*，构造的请求体如下，
  ``` json
   
  {
    "select": ["name", "fullPath", "size", "sha256", "md5", "metadata"],
    "page": {
      "pageNumber": 1,
      "pageSize": 20
    },
    "sort": {
      "properties": ["name", "size"],
      "direction": "ASC"
    },
    "rule": {
      "rules": [
        {
          "field": "projectId",
          "value": "test",
          "operation": "EQ"
        },
        {
          "field": "repoName",
          "value": ["generic-local1", "generic-local2"],
          "operation": "IN"
        },
        {
          "field": "name",
          "value": ".tgz",
          "operation": "SUFFIX"
        },
        {
          "field": "folder",
          "value": "false",
          "operation": "EQ"
        },
      ],
      "relation": "AND"
    }
  }
  ```



## 查询节点总览

- API: GET /repository/api/node/search/overview?projectId={projectId}&name={name}&exRepo={reponames}

- API 名称: query_node_overview

- 功能说明：

  - 中文：查询节点总览
    - English：query node overview

- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明                             | Description  |
  | --------- | ------ | -------- | ------ | -------------------------------- | ------------ |
  | projectId | string | 是       | 无     | 项目名称                         | project name |
  | name      | string | 是       | 无     | 查询文件名                       | file name    |
  | exRepo    | string | 否       | null   | 排除的仓库，多个仓库以 `,` 分隔" | repo name    |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": [
          {
              "projectId": "test",
              "repos": [
                  {
                      "repoName": "custom",
                      "packages": 10
                  },
                  {
                      "repoName": "scan_test",
                      "packages": 2
                  }
              ],
              "sum": 20
          }
      ],
      "traceId": ""
  }
  ```

- data字段说明

  | 字段      | 类型   | 说明         | Description          |
  | --------- | ------ | ------------ | -------------------- |
  | projectId | string | 节点所属项目 | node project id      |
  | repoName  | string | 节点所属仓库 | node repository name |
  | packages  | Int    | 节点数量     | node count           |

## 

## 清理创建时间早于{date}的文件节点

- API: DELETE /repository/api/node/clean/{projectId}/{repoName}?date=yyyy-MM-dd'T'HH:mm:ss.SSSXXX

- API 名称: delete_node_created_before_date

- 功能说明：

  - 中文：清理创建时间早于{date}的文件节点
  - English：delete node created before date

- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description  |
  | --------- | ------ | -------- | ------ | -------- | ------------ |
  | projectId | string | 是       | 无     | 项目名称 | project name |
  | repoName  | string | 是       | 无     | 仓库名称 | repo name    |
  | date      | string | 是       | 无     | 日期     | date time    |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 