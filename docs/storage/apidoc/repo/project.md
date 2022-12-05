# Project项目接口

[toc]

## 创建仓库

- API: POST /repository/api/project/create
- API 名称: create_project
- 功能说明：
  - 中文：创建项目
  - English：create project
- 请求体

  ```json
  {
    "name": "test",
    "displayName": "test",
    "description": "project description"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|项目名称，要求以字母或者下划线开头，长度不超过32位|proejct name|
  |displayName|string|是|无|显示名称，要求以字母或者下划线开头，长度不超过32位。此字段保留作用，和name设置为相同值即可|project display name|
  |description|string|是|无|项目描述|project description|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 查询项目列表

- API: GET /repository/api/project/list
- API 名称: get_project_list
- 功能说明：
  - 中文：查询项目列表
  - English：get project list
- 请求体
  此接口请求体为空
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |names|string|否|无|项目名称，多个以,隔开|project name|
  |displayNames|string|否|无|显示名称，多个以,隔开|project display name|
  |pageSize|int|否|无|分页数量|page size|
  |pageNumber|int|否|无|当前页|page number|
- 响应体

  ``` json
  {
      "code":0,
      "message":"",
      "data":[
          {
              "name":"project1",
              "displayName":"project1",
              "description":"project1"
          }
      ],
      "traceId": null
  }
  ```

- data 字段说明

  | 字段|类型|说明|Description|
  |---|---|---|---|
  |name|string|项目名|the project name |
  |displayName|string|项目展示名称|the display name of project|
  |description|string|项目描述|the description of project|
