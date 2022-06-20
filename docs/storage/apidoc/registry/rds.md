## RDS节点接口说明

### 删除 rds包

* API: DELETE  /rds/ext/package/delete/{project}/{repoName}/?packageKey={packageKey}

* API 名称: delete_rds

* 功能说明：

  - 中文：删除rds包，
  - English：delete rds 

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | packageKey | string | 是       | 无     | 包唯一key | package unique key |

* 响应体

  成功
  ```json
  {
      "code": 0,
      "message": null,
      "data": null,
      "traceId": ""
  }
  ```
  失败
  ```json
  {
    "error": "remove package rds://chart failed: no such file or directory"
  }
  ```
  

* data字段说明



### 删除对应版本rds包

* API: DELETE  /rds/ext/version/delete/{project}/{repoName}/?packageKey={packageKey}&version={version}
      
* API 名称: delete_rds_with_version

* 功能说明：

  - 中文：删除对应版本rds包，
  - English：delete rds with version

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

  成功
  ```json
  {
      "code": 0,
      "message": null,
      "data": null,
      "traceId": ""
  }
  ```
  失败
  ```json
  {
      "error": "remove package rds://chart failed: no such file or directory"
  }
  ```
  

* data字段说明


### 删除对应版本rds包

* API: DELETE  /rds/{project}/{repoName}/api/charts/{name}/{version}
* API 名称: delete_rds_with_version

* 功能说明：

  - 中文：删除对应版本rds包，
  - English：delete rds with version

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | version    | string | 是       | 无     | 版本名称  | version name       |
  | name       | string | 是       | 无     | 包名称    |  name              |


* 响应体
  
  成功
  ```json
  {
    "deleted": true
  }
  ```
  失败
   ```json
  {
    "error": "remove package rds://chart for version [1.0.0] failed: no such file or directory"
  }
  ```

  

* data字段说明



### 版本详情

* API: GET  /rds/ext/version/detail/{project}/{repoName}/?packageKey={packageKey}&version={version}

* API 名称: artifact_detail_detail

* 功能说明：

  - 中文：rds 版本详情
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
  
  成功
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
  失败
  ```json
  {
    "error": "node [/chart-1.0.0.tgz] don't found."
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

## rds 包上传

- API: POST /api/{projectId}/{repoName}/charts

- API 名称: rds 上传

- 功能说明：

  - 中文：rds 制品上传
  - English：rds chart push 

- 请求体:

  form-data :
  - key： chart
  - value： 文件

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明       | Description  |
  | --------- | ------ | -------- | ------ | ---------- | ------------ |
  | projectId | string | 是       | 无     | 项目名称   | project name |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |

- 响应体

  成功
  ```json
  {
    "saved": true
  }
  ```
  失败
  ```json
  {
    "error": "chart-1.0.0.zip already exists"
  }
  ```

- data字段说明

### 获取索引index

* API: GET  /rds/{project}/{repoName}/index.yaml

* API 名称: get index detail

* 功能说明：

  - 中文：获取rds仓库下索引文件信息
  - English：get index detail

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |

* 响应体
  成功
  ```
  apiVersion: "v1"
  entries:
    包名:
    - name: "XXX"
      code: "XXX"
      version: "XXX"
      description: "XXX"
      home: "XXXX"
      icon: "XXX"
      digest: "XXX"
      created: "XXX"
      extension: "XXX"
      keywords:
      - "XXXX"
      - "XXX"
      - "XXX"
      - "XXX"
      maintainers:
      - name: "XXXX"
        email: "XXX"
      rdsversion: "v1"
  - name: "XXX"
      code: "XXX"
      version: "XXX"
      description: "XXX"
      home: "XXXX"
      icon: "XXX"
      digest: "XXX"
      created: "XXX"
      extension: "XXX"
      keywords:
      - "XXXX"
      - "XXX"
      - "XXX"
      - "XXX"
      maintainers:
      - name: "XXXX"
        email: "XXX"
      rdsversion: "v1"
  generated: "2022-03-21T15:42:47.176Z"
  serverInfo: {}
  ```
  失败
  ```json
  {
    "error": "Repository [test-rds] not found"
  }
  ```

  | 字段             | 类型   | 说明                        | Description               |
  | ---------------- | ------ | --------------------------- | ------------------------- |
  | apiVersion          | String | apiVersion                     | apiVersion                   |
  | entries       | map | 包信息                  | rds info                |
  | generated          | String | 生成时间                     | generated time                   |
  

entries中key 为包name， value为包含包多个版本的详情set

### 获取特定包信息

* API: GET  /rds/{project}/{repoName}/api/charts/{name}/{version}

* API 名称: get rds list

* 功能说明：

  - 中文：获取特定包信息
  - English： get rds list

* 请求体
  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | name       | string | 可选       | 无     | 包名 | name unique key |
  | version    | string | 可选       | 无     | 版本名称  | version name       |

* 响应体
  当name与version不传，查询所有的包
  成功
  ```json
  {
    "包名": [
        {
            "name": "XXX",
            "code": "XXX",
            "version": "XXX",
            "description": "XXX",
            "home": "XXX",
            "icon": "XXX",
            "digest": "XXX",
            "created": "XXX",
            "extension": "zip",
            "keywords": [
                "XXX",
                "XXX",
                "XXX",
                "XXX"
            ],
            "maintainers": [
                {
                    "name": "XXX",
                    "email": "XXX"
                }
            ],
            "rdsversion": "v1"
        }
    ]
  }
  ```
  当version不传，查询指定name包的所有版本
  ```json
    [
      {
          "name": "XXX",
            "code": "XXX",
            "version": "XXX",
            "description": "XXX",
            "home": "XXX",
            "icon": "XXX",
            "digest": "XXX",
            "created": "XXX",
            "extension": "zip",
            "keywords": [
                "XXX",
                "XXX",
                "XXX",
                "XXX"
            ],
            "maintainers": [
                {
                    "name": "XXX",
                    "email": "XXX"
                }
            ],
            "rdsversion": "v1"
      }
    ]
    ```
    查询指定name包的指定version版本
    ```json
    {
        "name": "XXX",
                "code": "XXX",
                "version": "XXX",
                "description": "XXX",
                "home": "XXX",
                "icon": "XXX",
                "digest": "XXX",
                "created": "XXX",
                "extension": "zip",
                "keywords": [
                    "XXX",
                    "XXX",
                    "XXX",
                    "XXX"
                ],
                "maintainers": [
                    {
                        "name": "XXX",
                        "email": "XXX"
                    }
                ],
                "rdsversion": "v1"
    }
    ```
  失败
  ```json
  {
    "error": "Repository [test-rds] not found"
  }
  ```

  | 字段             | 类型   | 说明                        | Description               |
  | ---------------- | ------ | --------------------------- | ------------------------- |
  | apiVersion          | String | apiVersion                     | apiVersion                   |
  | entries       | map | 包信息                  | rds info                |
  | generated          | String | 生成时间                     | generated time                   |
  

entries中key 为包name， value为包含包多个版本的详情set

## 下载文件

- API: GET /rds/{project}/{repoName}/charts/{filename}
- API 名称: download
- 功能说明：
  - 中文：下载制品文件
  - English：download rds file

- 请求体
  此接口请求体为空

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |project|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|
  |filename|string|是|无|包名（格式为name-version.文件类型后缀）|file name|
 
- 响应体
  [文件流]