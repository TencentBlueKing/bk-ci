## Maven节点接口说明

### 删除 jar包

* API: DELETE  /maven/ext/package/delete/{project}/{repoName}/?packageKey={packageKey}

* API 名称: delete_jar

* 功能说明：

  - 中文：删除jar包，
  - English：delete jar 

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | packageKey | string | 是       | 无     | 包唯一key | package unique key |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": null,
      "traceId": ""
  }
  ```

  

* data字段说明



### 删除对应版本jar包

* API: DELETE  /maven/ext/version/delete/{project}/{repoName}/?packageKey={packageKey}&version={version}

* API 名称: delete_jar_with_version

* 功能说明：

  - 中文：删除对应版本jar包，
  - English：delete jar with version

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | packageKey | string | 是       | 无     | 包唯一key | package unique key |
  | version    | string | 是       | 无     | 版本名称  | version name       |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": null,
      "traceId": ""
  }
  ```

  

* data字段说明




### 版本详情

* API: GET  /maven/ext/package/detail/{project}/{repoName}/?packageKey={packageKey}&version={version}

* API 名称: artifact_detail_detail

* 功能说明：

  - 中文：maven jar 版本详情
  - English：artifact detail detail

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | packageKey | string | 是       | 无     | 包唯一key | package unique key |
  | version    | string | 是       | 无     | 版本名称  | version name       |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "basic": {
              "groupId": "com.tencent.bk.devops.atom",
              "artifactId": "bksdk",
              "version": "1.0.0",
              "size": 42786,
              "fullPath": "/com/tencent/bk/devops/atom/bksdk/1.0.0/bksdk-1.0.0.jar",
              "lastModifiedBy": "anonymous",
              "lastModifiedDate": "2020-09-27T14:43:06.083",
              "downloadCount": 10,
              "sha256": "e8e3e3b50daf0c2638a69e0b6ca3a1eee0b367bcabef5ae5bd7983700b9a6d58",
              "md5": "358d9a0f68b641bf0a25289d1795022b",
              "stageTag": [
                  "@release"
              ],
              "description": null
          },
          "metadata": {}
      },
      "traceId": ""
  }
  ```

* data字段说明

  | 字段             | 类型   | 说明                        | Description               |
  | ---------------- | ------ | --------------------------- | ------------------------- |
  | groupId          | String | groupId                     | groupId                   |
  | artifactId       | String | artifactId                  | artifactId                |
  | version          | String | version                     | version                   |
  | size             | long   | 节点大小                    | file size                 |
  | fullPath         | string | 节点完整路径                | node full path            |
  | folder           | bool   | 是否为文件夹                | is folder                 |
  | sha256           | string | 节点sha256                  | file sha256               |
  | md5              | array  | 节点md5                     | file md5 checksum         |
  | stageTag         | string | 晋级状态标签                | stage status tag          |
  | metadata         | object | 节点元数据，key-value键值对 | node metadata             |
  | downloadCount    | Int    | 下载次数                    | the download count of jar |
  | lastModifiedBy   | string | 上次修改者                  | last modify user          |
  | lastModifiedDate | string | 上次修改时间                | last modify time          |
  | description      | string | 描述信息                    | description               |

## GAVC 搜索接口

- API: GET /ext/search/gavc/{projectId}?g={groupId}&a={artifactId}&v={version}&c{classifier}&repos={repo[,repo]}

- API 名称: gavc_search

- 功能说明：

  - 中文：maven 制品 gavc 搜索
  - English：search maven artifact with gavc

- 请求体
  此接口请求体为空

- 请求字段说明

  **g ,a ,v ,c 四个查询条件不能全为空**

  | 字段      | 类型   | 是否必须 | 默认值 | 说明       | Description  |
  | --------- | ------ | -------- | ------ | ---------- | ------------ |
  | projectId | string | 是       | 无     | 项目名称   | project name |
  | g         | string | 否       | 无     | groupID    | groupID      |
  | a         | string | 否       | 无     | artifactId | artifactId   |
  | v         | string | 否       | 无     | version    | version      |
  | c         | string | 否       | 无     | classifier | classifier   |
  | repos     | string | 否       | 无     | 仓库名列表 | repo list    |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "pageNumber": 1,
          "pageSize": 20,
          "totalRecords": 4,
          "totalPages": 1,
          "records": [
              {
                  "uri": "http://127.0.0.1/maven/test/maven/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.jar"
              },
              {
                  "uri": "http://127.0.0.1/maven/test/maven/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.pom"
              },
              {
                  "uri": "http://127.0.0.1/maven/test/maven1/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.jar"
              },
              {
                  "uri": "http://127.0.0.1/maven/test/maven1/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.pom"
              }
          ],
          "page": 1,
          "count": 4
      },
      "traceId": ""
  }
  ```

- data字段说明

  | 字段 | 类型   | 说明         | Description  |
  | ---- | ------ | ------------ | ------------ |
  | uri  | string | 文件下载链接 | artifact uri |





