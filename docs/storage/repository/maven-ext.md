# maven 扩展接口



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

