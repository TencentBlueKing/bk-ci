# Replication仓库同步接口

[toc]

## 创建集群同步任务

- API: POST  /replication/api/task/create
- API 名称: create_replication_task
- 功能说明：
	- 中文：创建集群同步任务
	- English：create replication task
- 请求体:

  ```json
  {
    "name": "计划",
    "localProjectId": "bkrepo",
    "replicaObjectType": "REPOSITORY",
    "replicaTaskObjects": [
      {
        "localRepoName": "maven-local",
        "remoteProjectId": "bkrepo",
        "remoteRepoName": "maven-local",
        "repoType": "MAVEN",
        "packageConstraints": [
          {
            "packageKey": "gav://com.alibaba:fastjson",
            "versions": ["1.2.47","1.2.48"]
          }
        ],
        "pathConstraints": []
      }
    ],
    "replicaType": "SCHEDULED",
    "setting": {
      "rateLimit": 0,
      "includeMetadata": true,
      "conflictStrategy": "SKIP",
      "errorStrategy": "CONTINUE",
      "executionStrategy": "IMMEDIATELY",
      "executionPlan": {
        "executeImmediately": true
      }
    },
    "remoteClusterIds": ["651095dfe0524ce9b3ab53d13532361c","329fbcda45944fb9ae5c2573acd7bd2a"],
    "enabled": true,
    "description": "test replica task"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|计划名称|replication name|
  |localProjectId|string|是|无|本地项目ID|the local project Id|
  |replicaObjectType|enum|是|无|[REPOSITORY,PACKAGE,PATH]|replication object type|
  |replicaTaskObjects|object|是|无|同步对象信息|replication object info|
  |replicaType|enum|是|SCHEDULED|[SCHEDULED,REAL_TIME]|replication type|
  |setting|object|是|无|计划相关设置|task setting|
  |remoteClusterIds|list|是|无|远程集成节点id|the remote cluster node ids|
  |enabled|bool|是|true|计划是否启动|do task enabled|
  |description|sting|否|无|描述|description|
  
- replicaTaskObjects对象说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |localRepoName|string|是|无|本地仓库名称|the local repoName|
  |remoteProjectId|string|是|无|远程项目id|the remote project Id|
  |remoteRepoName|string|是|无|远程仓库名称|the remote repoName|
  |repoType|enum|是|无|[DOCKER,MAVEN,NPM, ...]|repository type|
  |packageConstraints|list|否|无|包限制|package constraints|
  |pathConstraints|list|否|无|路径限制|path constraints|
  
- setting对象说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |rateLimit|long|是|0|分发限速|rate limit|
  |includeMetadata|bool|是|true|是否同步元数据|do include metadata|
  |conflictStrategy|enum|是|SKIP|[SKIP,OVERWRITE,FAST_FAIL]|conflict strategy|
  |errorStrategy|enum|是|CONTINUE|[CONTINUE,FAST_FAIL]|error strategy|
  |executionStrategy|enum|是|IMMEDIATELY|[IMMEDIATELY,SPECIFIED_TIME,CRON_EXPRESSION]|execution strategy|
  |executionPlan|object|是|无|调度策略|execution plan|

- executionPlan对象说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |executeImmediately|bool|是|true|立即执行|execute immediately|
  |executeTime|time|否|无|执行时间执行|execute time|
  |cronExpression|string|否|无|cron表达式执行|cron expression|


- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "id": "609b573353ccce752bf9b85f",
      "key": "784e49c5ba974a1e8ac503a840f65eb5",
      "name": "测试分发计划",
      "projectId": "bkrepo",
      "replicaObjectType": "REPOSITORY",
      "replicaType": "SCHEDULED",
      "setting": {
        "rateLimit": 0,
        "includeMetadata": true,
        "conflictStrategy": "SKIP",
        "errorStrategy": "CONTINUE",
        "executionStrategy": "IMMEDIATELY",
        "executionPlan": {
          "executeImmediately": true
        }
      },
      "remoteClusters": [
        {
          "id": "651095dfe0524ce9b3ab53d13532361c",
          "name": "shanghai"
        },
        {
          "id": "329fbcda45944fb9ae5c2573acd7bd2a",
          "name": "beijing"
        }
      ],
      "description": "for test",
      "lastExecutionStatus": "SUCCESS",
      "lastExecutionTime": "2020-03-16T12:00:00.000",
      "nextExecutionTime": "2020-03-17T12:00:00.000",
      "executionTimes": 5,
      "enabled": true,
      "createdBy" : "system",
      "createdDate" : "2020-03-16T12:13:03.371",
      "lastModifiedBy" : "system",
      "lastModifiedDate" : "2020-03-16T12:13:03.371"
    },
    "traceId": null
  }
  ```

## 根据key查询任务信息

- API: GET /replication/api/task/info/{key}
- API 名称: get_task_info
- 功能说明：
  - 中文：查询任务信息
  - English：get task info
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
    "data": {
      "id": "609b573353ccce752bf9b85f",
      "key": "784e49c5ba974a1e8ac503a840f65eb5",
      "name": "测试分发计划",
      "projectId": "bkrepo",
      "replicaObjectType": "REPOSITORY",
      "replicaType": "SCHEDULED",
      "setting": {
        "rateLimit": 0,
        "includeMetadata": true,
        "conflictStrategy": "SKIP",
        "errorStrategy": "CONTINUE",
        "executionStrategy": "IMMEDIATELY",
        "executionPlan": {
          "executeImmediately": true
        }
      },
      "remoteClusters": [
        {
          "id": "651095dfe0524ce9b3ab53d13532361c",
          "name": "shanghai"
        },
        {
          "id": "329fbcda45944fb9ae5c2573acd7bd2a",
          "name": "beijing"
        }
      ],
      "description": "for test",
      "lastExecutionStatus": "SUCCESS",
      "lastExecutionTime": "2020-03-16T12:00:00.000",
      "nextExecutionTime": "2020-03-17T12:00:00.000",
      "executionTimes": 5,
      "enabled": true,
      "createdBy" : "system",
      "createdDate" : "2020-03-16T12:13:03.371",
      "lastModifiedBy" : "system",
      "lastModifiedDate" : "2020-03-16T12:13:03.371"
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|任务唯一id|task id|
  |key|string|任务唯一key|task key|
  |name|string|任务名称|task name|
  |projectId|string/所属项目id|task projectId|
  |replicaObjectType|enum|是|无|[REPOSITORY,PACKAGE,PATH]|replication object type|
  |replicaType|enum|[SCHEDULED,REAL_TIME]|replica type|
  |setting|object|计划相关设置|task setting|
  |remoteClusters|set|远程集成节点信息|the remote cluster node info|
  |description|string|任务描述信息|task description|
  |lastExecutionStatus|enum|[RUNNING,SUCCESS,FAILED]|task last execution status|
  |lastExecutionTime|date|上次执行时间|task last execution time|
  |nextExecutionTime|date|下次执行时间|task next execution time|
  |executionTimes|long|执行次数|execution times|
  |enabled|bool|是|true|计划是否启动|do task enabled|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|

## 根据key查询任务详情

- API: GET /replication/api/task/detail/{key}
- API 名称: get_task_detail
- 功能说明：
  - 中文：查询任务信息
  - English：get task detail
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
    "data": {
      "task": {
          "id": "609b573353ccce752bf9b85f",
          "key": "784e49c5ba974a1e8ac503a840f65eb5",
          "name": "测试分发计划",
          "projectId": "bkrepo",
          "replicaObjectType": "REPOSITORY",
          "replicaType": "SCHEDULED",
          "setting": {
            "rateLimit":0,
            "includeMetadata":true,
            "conflictStrategy": "SKIP",
            "errorStrategy": "CONTINUE",
            "executionStrategy": "IMMEDIATELY",
            "executionPlan":{
              "executeImmediately":true
            }
          },
          "remoteClusters": [
            {
              "id": "651095dfe0524ce9b3ab53d13532361c",
              "name": "shanghai"
            },
            {
              "id": "329fbcda45944fb9ae5c2573acd7bd2a",
              "name": "beijing"
            }
          ],
          "description": "for test",
          "lastExecutionStatus": "SUCCESS",
          "lastExecutionTime": "2020-03-16T12:00:00.000",
          "nextExecutionTime": "2020-03-17T12:00:00.000",
          "executionTimes": 5,
          "enabled": true,
          "createdBy" : "system",
          "createdDate" : "2020-03-16T12:13:03.371",
          "lastModifiedBy" : "system",
          "lastModifiedDate" : "2020-03-16T12:13:03.371"
      },
      "objects": [
        {
          "localRepoName": "npm-local",
          "remoteProjectId": "bkrepo",
          "remoteRepoName": "npm-local",
          "repoType": "NPM",
          "packageConstraints": [
            {
              "packageKey": "npm://helloworld",
              "versions": ["1.0.0","1.0.1"]
            }
          ],
          "pathConstraints": []
        }    
      ]
    },
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |task|object|同步任务基础信息|task info|
  |objects|list|同步对象列表|task objects|

- task字段说明
  同上

- objects字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |localRepoName|string|本地仓库名称|local repo name|
  |remoteProjectId|string|远程项目id|remote project id|
  |remoteRepoName|string|远程仓库名称|remote repo name|
  |repoType|enum|[MAVEN,DOCKER,NPM,...]|local repo type|
  |packageConstraints|list|包限制|package constraints|
  |pathConstraints|list|路径限制|path constraints|

## 分页查询任务

- API: GET /replication/api/task/page/{projectId}?name=test&lastExecutionStatus=SUCCESS&enabled=true&sortType=CREATE_TIME&pageNumber=0&pageSize=20
- API 名称: list_task_page
- 功能说明：
  - 中文：分页查询任务
  - English：list task page
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目id|project id|
  |name|string|否|无|任务名称，支持前缀模糊匹配|task name|
  |lastExecutionStatus|enum|否|无|上次执行状态|last execution status|
  |enabled|bool|否|无|任务启停状态|do task enabled|
  |sortType|enum|否|CREATED_TIME|[CREATED_TIME,LAST_EXECUTION_TIME,NEXT_EXECUTION_TIME]|sort by time|
  |pageNumber|int|是|无|当前页|page number|
  |pageSize|int|是|无|分页数量|page size|

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
          "id": "609b573353ccce752bf9b85f",
          "key": "784e49c5ba974a1e8ac503a840f65eb5",
          "name": "testTask",
          "projectId": "bkrepo",
          "replicaObjectType": "REPOSITORY",
          "replicaType": "SCHEDULED",
          "setting": {
            "rateLimit":0,
            "includeMetadata":true,
            "conflictStrategy": "SKIP",
            "errorStrategy": "CONTINUE",
            "executionStrategy": "IMMEDIATELY",
            "executionPlan": {
              "executeImmediately":true
            }
          },
          "remoteClusters": [
            {
              "id": "651095dfe0524ce9b3ab53d13532361c",
              "name": "shanghai"
            },
            {
              "id": "329fbcda45944fb9ae5c2573acd7bd2a",
              "name": "beijing"
            }
          ],
          "description": "for test",
          "lastExecutionStatus": "SUCCESS",
          "lastExecutionTime": "2020-03-16T12:00:00.000",
          "nextExecutionTime": "2020-03-17T12:00:00.000",
          "executionTimes": 5,
          "enabled": true,
          "createdBy" : "system",
          "createdDate" : "2020-03-16T12:13:03.371",
          "lastModifiedBy" : "system",
          "lastModifiedDate" : "2020-03-16T12:13:03.371"
        }
      ]
    },
    "traceId": null
  }
  ```

- data字段说明

  同上
  
## 删除任务

- API: DELETE /replication/api/task/delete/{key}
- API 名称: delete_task
- 功能说明：
  - 中文：删除任务
  - English：delete task
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
    "data": null,
    "traceId": null
  }
  ```
  
## 任务启停状态切换

- API: POST /replication/api/task/toggle/status/{key}
- API 名称: toggle_status_task
- 功能说明：
  - 中文：任务启停状态切换
  - English：task toggle status
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
    "data": null,
    "traceId": null
  }
  ```
  
## 复制集群同步任务

- API: POST  /replication/api/task/copy
- API 名称: copy_replication_task
- 功能说明：
	- 中文：复制集群同步任务
	- English：copy replication task
- 请求体:

  ```json
  {
    "name": "task_copy",
    "key": "e8d095dfe0524ce9b3ab53d1353239h8",
    "description": "copy replica task"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|计划名称|replication name|
  |key|string|是|无|任务唯一key|task unique key|
  |description|sting|否|无|描述|description|
  
- 响应体

  ```
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 更新同步任务

- API: POST  /replication/api/task/update
- API 名称: update_replication_task
- 功能说明：
	- 中文：更新集群同步任务
	- English：update replication task
- 请求体:

  ```json
  {
    "key": "计划唯一key",
    "name": "更新后的名称",
    "localProjectId": "bkrepo",
    "replicaObjectType": "REPOSITORY",
    "replicaTaskObjects": [
      {
        "localRepoName": "maven-local",
        "remoteProjectId": "bkrepo",
        "remoteRepoName": "maven-local",
        "repoType": "MAVEN",
        "packageConstraints": [
          {
            "packageKey": "gav://com.alibaba:fastjson",
            "versions": ["1.2.47","1.2.48"]
          }
        ],
        "pathConstraints": []
      }
    ],
    "remoteClusterIds": ["651095dfe0524ce9b3ab53d13532361c","329fbcda45944fb9ae5c2573acd7bd2a"],
    "description": "test replica task"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |key|string|是|否|唯一key|replication task key|
  |name|string|是|无|计划名称|replication name|
  |localProjectId|string|是|无|本地项目ID|the local project Id|
  |replicaObjectType|enum|是|无|[REPOSITORY,PACKAGE,PATH]|replication object type|
  |replicaTaskObjects|object|是|无|同步对象信息|replication object info|
  |remoteClusterIds|list|是|无|远程集成节点id|the remote cluster node ids|
  |description|sting|否|无|描述|description|
  
- replicaTaskObjects对象说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |localRepoName|string|是|无|本地仓库名称|the local repoName|
  |remoteProjectId|string|是|无|远程项目id|the remote project Id|
  |remoteRepoName|string|是|无|远程仓库名称|the remote repoName|
  |repoType|enum|是|无|[DOCKER,MAVEN,NPM, ...]|repository type|
  |packageConstraints|list|否|无|包限制|package constraints|
  |pathConstraints|list|否|无|路径限制|path constraints|
  
- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 手动执行同步任务

- API: POST  /replication/api/task/execute/{key}
- API 名称: execute_replication_task
- 功能说明：
	- 中文：手动执行集群同步任务
	- English：execute replication task
- 请求体:
  此接口无请求体

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |key|string|是|否|唯一key|replication task key|
  
- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```
