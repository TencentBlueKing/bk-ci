# Replication仓库同步执行日志接口

[toc]

## 根据recordId查询任务执行日志

- API: GET /replication/api/task/record/{recordId}
- API 名称: get_task_record
- 功能说明：
  - 中文：查询任务信息
  - English：get task record
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |recordId|string|是|无|记录唯一key|record id|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "replicaObjectType": "REPOSITORY",    
      "record": {
        "id": "609b573d53ccce752bf9b860",
        "taskKey": "651095dfe0524ce9b3ab53d13532361c",
        "status": "SUCCESS",
        "startTime": "2021-05-12T12:19:08.813",
        "endTime": "2021-05-12T12:19:37.967",
        "errorReason": null
      }
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |replicaObjectType|enum|[REPOSITORY,PACKAGE,PATH]|replica object type|
  |id|string|执行日志唯一id|record id|
  |taskKey|string|任务唯一key|task key|
  |status|enum|[RUNNING,SUCCESS,FAILED]|task status|
  |startTime|date|任务开始执行时间|task execute start time|
  |endTime|date|任务结束执行时间|task execute end time|
  |errorReason|string|错误原因，未执行或执行成功则为null|task failed error reason|

## 根据key查询任务执行日志列表

- API: GET /replication/api/task/record/list/{key}
- API 名称: list_task_record
- 功能说明：
  - 中文：查询任务信息列表
  - English：list task record
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |key|string|是|无|任务唯一key|task key|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
      {
        "id": "609b573d53ccce752bf9b860",
        "taskKey": "651095dfe0524ce9b3ab53d13532361c",
        "status": "SUCCESS",
        "startTime": "2021-05-12T12:19:08.813",
        "endTime": "2021-05-12T12:19:37.967",
        "errorReason": null
      }
    ],
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|执行日志唯一id|record id|
  |taskKey|string|任务唯一key|task key|
  |status|enum|[RUNNING,SUCCESS,FAILED]|task status|
  |startTime|date|任务开始执行时间|task execute start time|
  |endTime|date|任务结束执行时间|task execute end time|
  |errorReason|string|错误原因，未执行或执行成功则为null|task failed error reason|

## 根据key分页查询任务执行日志列表

- API: GET /replication/api/task/record/page/{key}
- API 名称: list_task_record_page
- 功能说明：
  - 中文：分页查询任务日志列表
  - English：list task record page
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |key|string|是|无|任务唯一id|task id|
  |pageNumber|int|是|无|当前页|page number|
  |pageSize|int|是|无|分页数量|page size|
  |status|enum|否|无|执行状态|execute status|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "pageNumber": 0,
      "pageSize": 1,
      "totalRecords": 8,
      "totalPages": 2,
      "records": [
        {
          "id": "609b573d53ccce752bf9b860",
          "taskKey": "651095dfe0524ce9b3ab53d13532361c",
          "status": "SUCCESS",
          "startTime": "2021-05-12T12:19:08.813",
          "endTime": "2021-05-12T12:19:37.967",
          "errorReason": null
        }
      ]
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|执行日志唯一id|record id|
  |taskKey|string|任务唯一key|task key|
  |status|enum|[RUNNING,SUCCESS,FAILED]|task status|
  |startTime|date|任务开始执行时间|task execute start time|
  |endTime|date|任务结束执行时间|task execute end time|
  |errorReason|string|错误原因，未执行或执行成功则为null|task failed error reason|


## 根据recordId查询任务执行日志详情列表

- API: GET /replication/api/task/record/detail/list/{recordId}
- API 名称: list_task_record_detail
- 功能说明：
  - 中文：查询任务日志详情列表
  - English：list task record detail
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |recordId|string|是|无|任务执行日志唯一id|record id|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
      {
        "id": "979b573d53efcd752bf9b762",
        "recordId": "609b573d53ccce752bf9b860",
        "localCluster": "651095dfe0524ce9b3ab53d13532361c",
        "remoteCluster": "SUCCESS",
        "localRepoName": "npm-local",
        "repoType": "NPM",
        "packageConstraint": {
          "packageKey": "npm://helloworld",
          "versions": ["1.1.0","1.3.0"]
        },
        "pathConstraint": null,
        "status": "SUCCESS",
        "progress": {
          "success": 10,
          "skip": 0,
          "failed": 0,
          "totalSize": 100
        },
        "startTime": "2021-05-12T12:19:08.813",
        "endTime": "2021-05-12T12:19:37.967",
        "errorReason": null
      }
    ],
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|记录详情唯一id|record detail id|
  |recordId|string|记录唯一id|record id|
  |localCluster|string|本地集群名称|local cluster node name|
  |remoteCluster|string|远程集群名称|remote cluster node name|
  |localRepoName|string|本地仓库名称|local repository name|
  |repoType|enum|[DOCKER,NPM,RPM,...]|local repository type|
  |packageConstraints|object|否|无|包限制|package constraints|
  |pathConstraints|object|否|无|路径限制|path constraints|
  |status|enum|[RUNNING,SUCCESS,FAILED]|task execute status|
  |progress|object|同步进度|task execute progress|
  |startTime|date|任务开始执行时间|task execute start time|
  |endTime|date|任务结束执行时间|task execute end time|
  |errorReason|string|错误原因，未执行或执行成功则为null|task failed error reason|

- progress字段说明
  
  |字段|类型|说明|Description|
  |---|---|---|---|
  |success|long|成功数量|success size|
  |skip|long|跳过数量|skip size|
  |failed|long|失败数量|failed size|
  |totalSize|long|数据总量|total size|
  
## 根据recordId分页查询任务执行日志详情列表

- API: GET /replication/api/task/record/detail/page/{recordId}
- API 名称: list_task_record_detail_page
- 功能说明：
  - 中文：分页查询任务日志详情列表
  - English：list task record detail page
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |recordId|string|是|无|任务执行日志唯一id|record id|
  |pageNumber|int|是|无|当前页|page number|
  |pageSize|int|是|无|分页数量|page size|
  |packageName|string|否|无|包名称，支持前缀模糊匹配|package name|
  |repoName|string|否|无|仓库名称|repo name|
  |clusterName|string|否|无|远程节点名称|cluster node name|
  |path|string|否|无|路径名称，支持前缀模糊匹配|file path|
  |status|enum|否|无|[SUCCESS,RUNNING,FAILED]|execute status|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "pageNumber": 0,
      "pageSize": 1,
      "totalRecords": 18,
      "totalPages": 2,
      "records": [
        {
          "id": "979b573d53efcd752bf9b762",
          "recordId": "609b573d53ccce752bf9b860",
          "localCluster": "wuxi",
          "remoteCluster": "wuhan",
          "localRepoName": "npm-local",
          "repoType": "NPM",
          "packageConstraint": {
            "packageKey": "npm://helloworld",
            "versions": ["1.1.0","1.3.0"]
          },
          "pathConstraint": {
            "path": "/busy/box.txt"
          },
          "status": "SUCCESS",
          "progress": {
            "success": 10,
            "skip": 0,
            "failed": 0,
            "totalSize": 10
          },
          "startTime": "2021-05-12T12:19:08.813",
          "endTime": "2021-05-12T12:19:37.967",
          "errorReason": null
        }
      ]
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|记录详情唯一id|record detail id|
  |recordId|string|记录唯一id|record id|
  |localCluster|string|本地集群名称|local cluster node name|
  |remoteCluster|string|远程集群名称|remote cluster node name|
  |localRepoName|string|本地仓库名称|local repository name|
  |repoType|enum|[DOCKER,NPM,RPM,...]|local repository type|
  |packageConstraints|object|否|无|包限制|package constraints|
  |pathConstraints|object|否|无|路径限制|path constraints|
  |status|enum|[RUNNING,SUCCESS,FAILED]|task execute status|
  |progress|object|同步进度|task execute progress|
  |startTime|date|任务开始执行时间|task execute start time|
  |endTime|date|任务结束执行时间|task execute end time|
  |errorReason|string|错误原因，未执行或执行成功则为null|task failed error reason|

- progress字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |success|long|成功数量|success size|
  |skip|long|跳过数量|skip size|
  |failed|long|失败数量|failed size|
  |totalSize|long|数据总量|total size|
