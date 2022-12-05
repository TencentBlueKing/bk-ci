## NPM节点接口说明

NPM节点接口使用统一接口协议，公共部分请参照[通用接口协议说明](./common.md)

### 查询包的版本详情

- API: GET /npm/ext/version/detail/{projectId}/{repoName}?packageKey=xxx&version=xxx

- API 名称: package_version_detail

- 功能说明: 
    - 中文：查询NPM包版本详情信息
    - English：package version detail
- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | packageKey | string | 是       | 无     | 包唯一Key | package unique Key |
  | version    | string | 是       | 无     | 包版本    | package version    |

- 响应体

```json
{
    "code": 0,
    "message": null,
    "data": {
        "basic": {
            "version": "1.0.0",
            "fullPath": "/helloworld/-/helloworld-1.0.0.tgz",
            "size": 416,
            "sha256": "d89fa4a0516258d994db19d0bb3aec099f98b9d6d7554c1da66db97b50b535c1",
            "md5": "57616ff6cdec6db95c9b2f3fae259185",
            "stageTag": [
              "@prerelease",
              "@release"
            ],
            "projectId": "test",
            "repoName": "npm-test",
            "downloadCount": 0,
            "createdBy": "admin",
            "createdDate": "2020-09-28T17:29:04.814",
            "lastModifiedBy": "admin",
            "lastModifiedDate": "2020-09-28T17:29:04.814"
        },
        "metadata": {},
        "dependencyInfo": {
            "dependencies": [
                {
                    "name": "underscore",
                    "version": "^1.11.0"
                },
                {
                    "name": "@hapi/hoek",
                    "version": "^9.0.0"
                }
            ],
            "devDependencies": [
                {
                    "name": "@hapi/code",
                    "version": "8.x.x"
                },
                {
                    "name": "@hapi/lab",
                    "version": "22.x.x"
                }
            ],
            "dependents": {
                "pageNumber": 1,
                "pageSize": 20,
                "totalRecords": 1,
                "totalPages": 1,
                "records": [
                    {
                        "createdBy": "admin",
                        "createdDate": "2020-09-26T17:56:05.395",
                        "name": "helloworld",
                        "deps": "@xwhy/charis",
                        "projectId": "test",
                        "repoName": "npm-test"
                    }
                ],
                "count": 1,
                "page": 1
            }
        }
    },
    "traceId": ""
}
```

- data字段说明

  - Basic字段

  | 字段             | 类型   | 说明             | Description          |
  | ---------------- | ------ | ---------------- | -------------------- |
  | version          | string | 包对应版本       | project id           |
  | fullPath         | string | 包对应仓库全路径 | fullPath             |
  | size             | string | 包大小           | file size            |
  | sha256           | string | 包的sha256值     | file sha256          |
  | md5              | string | 节点md5值        | file md5             |
  | stageTag         | string | 晋级状态标签     | stage status tag     |
  | projectId        | string | 节点所属项目     | node project id      |
  | repoName         | string | 节点所属仓库     | node repository name |
  | downloadCount    | string | 简要描述         | download times       |
  | createdBy        | string | 创建者           | create user          |
  | createdDate      | string | 创建时间         | create time          |
  | lastModifiedBy   | string | 上次修改者       | last modify user     |
  | lastModifiedDate | string | 上次修改时间     | last modify time     |

  - Metadata字段：

  | 字段 | 类型 | 说明 | Description |
  | ---- | ---- | ---- | ----------- |
  |      |      |      |             |

  - dependencyInfo字段：

    - dependencies: 依赖信息

    | 字段 | 类型 | 说明 | Description |
    | ----    | ----   | ----        | -----------                  |
    | name    | string | 依赖的包名称  | dependencies package name    |
    | version | string | 依赖的包版本  | dependencies package version |

    - devDependencies: 开发依赖信息

    | 字段 | 类型 | 说明 | Description |
    | ----    | ----   | ----           | -----------                     |
    | name    | string | 开发依赖的包名称  | devDependencies package name    |
    | version | string | 开发依赖的包版本  | devDependencies package version |

    - dependents: 被依赖信息

    | 字段        | 类型   | 说明           | Description            |
    | ----------- | ------ | -------------- | ---------------------- |
    | name        | string | 包名称         | package name           |
    | deps        | string | 被依赖的包名称 | package dependent name |
    | projectId   | string | 节点所属项目   | node project id        |
    | repoName    | string | 节点所属仓库   | node repository name   |
    | createdBy   | string | 创建者         | create user            |
    | createdDate | string | 创建时间       | create time            |



### 删除仓库下的包

- API: DELETE /npm/ext/package/delete/{projectId}/{repoName}?packageKey=npm://helloworld
- API 名称: delete_package
- 功能说明：
  - 中文：删除包
  - English：delete package
- 请求体
  此接口请求体为空
- 请求字段说明

| 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
| ---------- | ------ | -------- | ------ | --------- | ------------------ |
| projectId  | string | 是       | 无     | 项目名称  | project name       |
| repoName   | string | 是       | 无     | 仓库名称  | repo name          |
| packageKey | string | 是       | 无     | 包唯一key | package unique key |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": ""
  }
  ```
  
- record字段说明

  请求成功无返回数据



### 删除包版本

- API: DELETE /npm/ext/version/delete/{projectId}/{repoName}?packageKey=npm://helloworld&version=1.0.1
- API 名称: delete_version
- 功能说明：

  - 中文：删除版本
  - English：delete version
- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明       | Description        |
  | ---------- | ------ | -------- | ------ | ---------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称   | project name       |
  | repoName   | string | 是       | 无     | 仓库名称   | repo name          |
  | packageKey | string | 是       | 无     | 包唯一key  | package unique key |
  | version    | string | 是       | 无     | 包版本名称 | version name       |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": ""
  }
  ```

- record字段说明

  请求成功无返回数据

