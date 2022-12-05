# 扫描接口

[toc]

## 创建扫描任务

- API: POST /scanner/api/scan
- API 名称: scan
- 功能说明：
    - 中文：发起扫描
    - English：scan
- 请求体

```json
{
  "scanner": "default",
  "rule": {
    "relation": "AND",
    "rules": [
      {
        "field": "projectId",
        "value": "testProjectId",
        "operation": "EQ"
      },
      {
        "field": "repoName",
        "value": "maven-local",
        "operation": "EQ"
      },
      {
        "field": "fullPath",
        "value": "/",
        "operation": "PREFIX"
      }
    ]
  }
}
```

- 请求字段说明

| 字段      | 类型     | 是否必须 | 默认值 | 说明                                                         | Description     |
|---------|--------|------|-----|------------------------------------------------------------|-----------------|
| scanner | string | 是    | 无   | 要获取的报告使用的扫描器名称                                             | scanner name    |
| rule    | object | 是    | 无   | 要扫描的文件匹配规则，参考[自定义搜索接口公共说明](../common/search.md?id=自定义搜索协议) | file match rule |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": {
    "taskId": "622aca02e01725126f31f7ce",
    "createdBy": "admin",
    "triggerDateTime": "2022-03-11T12:03:14.283",
    "startDateTime": null,
    "finishedDateTime": null,
    "status": "PENDING",
    "rule": {
      "rules": [
        {
          "field": "projectId",
          "value": "testProjectId",
          "operation": "EQ"
        },
        {
          "field": "repoName",
          "value": "maven-local",
          "operation": "EQ"
        },
        {
          "field": "fullPath",
          "value": "/",
          "operation": "PREFIX"
        }
      ],
      "relation": "AND"
    },
    "total": 0,
    "scanning": 0,
    "failed": 0,
    "scanned": 0,
    "scanner": "default",
    "scannerType": "arrowhead",
    "scannerVersion": "1::1",
    "scanResultOverview": null
  },
  "traceId": ""
}
```

- data字段说明

| 字段                 | 类型     | 说明                                                         | Description            |
|--------------------|--------|------------------------------------------------------------|------------------------|
| taskId             | string | 任务id                                                       | task id                |
| createdBy          | string | 任务创建者                                                      | task creator           |
| triggerDatetime    | string | 触发任务的时间                                                    | task trigger time      |
| startDateTime      | string | 任务开始执行时间                                                   | task started time      |
| finishedDateTime   | string | 任务执行结束时间                                                   | task finished time     |
| status             | string | 任务状态                                                       | task status            |
| rule               | object | 要扫描的文件匹配规则，参考[自定义搜索接口公共说明](../common/search.md?id=自定义搜索协议) | file match rule        |
| total              | number | 总扫描文件数                                                     | total scan file count  |
| failed             | number | 扫描失败文件数                                                    | scan failed file count |
| scanned            | number | 已扫描文件数                                                     | scanned file count     |
| scanner            | string | 使用的扫描器明                                                    | scanner name           |
| scannerType        | string | 扫描器类型                                                      | scanner type           |
| scannerVersion     | string | 扫描器版本                                                      | scanner version        |
| scanResultOverview | array  | 扫描结果预览                                                     | scan result overview   |

扫描结果预览字段参考[支持的扫描器](./supported-scanner.md)

## 获取扫描任务

- API: GET /scanner/api/scan/tasks/{taskId}
- API 名称: get_task
- 功能说明：
    - 中文：获取扫描任务
    - English：get scan task
- 请求体 此接口请求体为空

- 请求字段说明

| 字段     | 类型     | 是否必须 | 默认值 | 说明   | Description |
|--------|--------|------|-----|------|-------------|
| taskId | string | 是    | 无   | 任务id | task id     |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": {
    "taskId": "622aca02e01725126f31f7ce",
    "createdBy": "admin",
    "triggerDateTime": "2022-03-11T12:03:14.283",
    "startDateTime": "2022-03-11T12:03:16.542",
    "finishedDateTime": "2022-03-11T13:09:23.319",
    "status": "FINISHED",
    "rule": {
      "rules": [
        {
          "field": "projectId",
          "value": "testProjectId",
          "operation": "EQ"
        },
        {
          "field": "repoName",
          "value": "maven-local",
          "operation": "EQ"
        },
        {
          "field": "fullPath",
          "value": "/",
          "operation": "PREFIX"
        }
      ],
      "relation": "AND"
    },
    "total": 1307,
    "scanning": 0,
    "failed": 0,
    "scanned": 1307,
    "scanner": "default",
    "scannerType": "arrowhead",
    "scannerVersion": "1::1",
    "scanResultOverview": {
      "sensitiveUriCount": 4
    }
  },
  "traceId": ""
} 
```

- data字段说明

响应体参考[创建扫描任务](./scan.md?id=创建扫描任务)响应体

## 分页获取扫描任务

- API: GET /scanner/api/scan/tasks
- API 名称: get_tasks
- 功能说明：
  - 中文：分页获取扫描任务
  - English：get scan tasks
- 请求体 此接口请求体为空

- 请求字段说明

| 字段         | 类型     | 是否必须 | 默认值 | 说明   | Description |
|------------|--------|------|-----|------|-------------|
| pageSize   | number | 否    | 20  | 分页大小 | page size   |
| pageNumber | number | 否    | 1   | 分页页码 | page number |

- 响应体

```json
{
    "code": 0,
    "message": null,
    "data": {
        "pageNumber": 1,
        "pageSize": 20,
        "totalRecords": 1,
        "totalPages": 1,
        "records": [
            {
                "taskId": "62277d0ff6671d535feaa286",
                "createdBy": "admin",
                "triggerDateTime": "2022-03-08T23:58:07.319",
                "startDateTime": "2022-03-10T11:40:04.808",
                "finishedDateTime": null,
                "status": "SCANNING_SUBMITTED",
                "rule": {
                    "rules": [
                        {
                            "field": "projectId",
                            "value": "testProjectId",
                            "operation": "EQ"
                        },
                        {
                            "field": "repoName",
                            "value": "test",
                            "operation": "EQ"
                        },
                        {
                            "field": "fullPath",
                            "value": "/",
                            "operation": "PREFIX"
                        }
                    ],
                    "relation": "AND"
                },
                "total": 1,
                "scanning": 1,
                "failed": 0,
                "scanned": 0,
                "scanner": "default",
                "scannerType": "arrowhead",
                "scannerVersion": "1::1",
                "scanResultOverview": null
            }
        ],
        "count": 1,
        "page": 1
    },
    "traceId": ""
}
```
- data字段说明

响应体参考[分页接口响应格式](../common/common.md?id=统一分页接口响应格式)

响应体参考[创建扫描任务](./scan.md?id=创建扫描任务)响应体
