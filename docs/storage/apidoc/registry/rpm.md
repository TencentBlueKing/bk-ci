### 创建仓库

- API: POST /repository/api/repo/create

- API 名称: create_repo

- 功能说明：

  - 中文：创建仓库
    - English：create repo

- 请求体

  ```json
  {
    "projectId": "test",
    "name": "generic-local",
    "type": "RPM",
    "category": "COMPOSITE",
    "public": false,
    "description": "repo description",
    "configuration": {
      "settings": {
              "enabledFileLists": false,
              "repodataDepth": 0,
              "groupXmlSet": []
          }
    },
    "storageCredentialsKey": null
  }
  ```

- 请求字段说明

  | 字段                  | 类型    | 是否必须 | 默认值    | 说明                  | Description             |
  | --------------------- | ------- | -------- | --------- | --------------------- | ----------------------- |
  | projectId             | string  | 是       | 无        | 项目名称              | project name            |
  | name                  | string  | 是       | 无        | 仓库名称              | repo name               |
  | type                  | string  | 是       | 无        | 仓库类型，枚举值      | repo type               |
  | category              | string  | 否       | COMPOSITE | 仓库类别，枚举值      | repo category           |
  | public                | boolean | 否       | false     | 是否公开              | is public repo          |
  | description           | string  | 否       | 无        | 仓库描述              | repo description        |
  | configuration         | object  | 否       | 无        | 仓库配置，参考后文    | repo configuration      |
  | storageCredentialsKey | string  | 否       | 无        | 存储凭证key           | storage credentials key |
  | enabledFileLists      | Boolean | 是       | false     | 是否启用filelists索引 | enabledFileLists        |
  | repodataDepth         | Int     | 是       | 0         | 索引目录深度          | repodataDepth           |
  | groupXmlSet           | String  | 否       | 无        | 分组文件列表          | groupXmlSet             |

  

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": ""
  }
  ```

- data字段说明

  请求成功无返回数据