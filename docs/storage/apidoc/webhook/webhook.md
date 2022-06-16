## Webhook接口

### Webhook公共请求参数说明

#### 触发器类型

| 枚举值           | 说明        |
| ---------------- | ----------- |
| PROJECT_CREATED  | 项目创建    |
| REPO_CREATED     | 仓库创建    |
| REPO_UPDATED     | 仓库更新    |
| NODE_CREATED     | 节点创建    |
| NODE_RENAMED     | 节点重命名  |
| NODE_MOVED       | 节点移动    |
| NODE_COPIED      | 节点复制    |
| NODE_DELETED     | 节点删除    |
| METADATA_DELETED | 元数据删除  |
| METADATA_SAVED   | 元数据保存  |
| VERSION_CREATED  | 版本创建    |
| WEBHOOK_TEST     | webhook测试 |

#### 关联对象类型

| 枚举值  | 说明 |
| ------- | ---- |
| SYSTEM  | 系统 |
| PROJECT | 项目 |
| REPO    | 仓库 |

#### 关联对象Id

association_type为PROJECT时，association_id为{projectId}

association_type为REPO时，association_id为{projectId}:{repoName}

### 创建webhook

- API: POST /webhook/api/webhook/create

- API名称: create_webhook

- 功能说明：

    - 中文：创建webhook
    - English：create webhook

- 请求体

  ```json
  {
    "url": "string",
    "headers": {"key": "value"},
    "triggers": ["NODE_CREATED"],
    "resourceKeyPattern": "regex pattern",
    "association_type": "REPO",
    "association_id": "string"
  }
  ```

  | 字段             | 是否必须 | 说明            | 示例                      |
  | ---------------- |----------| --------------- | ------------------------- |
  | url              | 是       | webhook请求地址   | http://bkrepo.example.com |
  | headers                | 否       | 自定义请求头        | {"key" : "value"}     |
  | triggers         | 是       | 触发事件          | ["NODE_CREATED"]          |
  | resourceKeyPattern | 否 | 事件资源key正则匹配模型 | (.*).apk                   |
  | association_type | 是       | 关联对象类型        | REPO                      |
  | association_id   | 否       | 关联对象id        | projectId:repoName        |


- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

### 更新webhook

- API: PUT /webhook/api/webhook/update

- API名称: update_webhook

- 功能说明：

    - 中文：更新webhook
    - English：update webhook

- 请求体

  ```json
  {
      "id": "string",
      "url": "string",
      "token": "string",
      "triggers": ["NODE_CREATED"]
  }
  ```

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

### 删除webhook

- API: DELETE /webhook/api/webhook/delete/{id}

- API名称: delete_webhook

- 功能说明：

    - 中文：删除webhook
    - English：delete webhook

- 请求参数

  | 参数名 | 类型   | 说明       |
  | ------ | ------ | ---------- |
  | id     | string | webhook id |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

### 查询webhook

- API: GET /webhook/api/webhook/{id}

- API名称: get_webhook

- 功能说明：

    - 中文：创建webhook
    - English：create webhook

- 请求参数

  | 参数名 | 类型   | 说明       |
  | ------ | ------ | ---------- |
  | id     | string | webhook id |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
        "id": "string",
        "url": "string",
        "token": "string",
        "triggers": ["NODE_CREATED"],
        "associationType": "REPO",
        "associationId": "string",
        "createdBy": "string",
        "createdDate": "string",
        "lastModifiedBy": "string",
        "lastModifiedDate": "string"
  	},
    "traceId": null
  }
  ```

### 查询webhook列表

- API: GET /webhook/api/webhook/list

- API名称: list_webhook

- 功能说明：

    - 中文：查询webhook列表
    - English：list webhook

- 请求参数

  | 参数名          | 类型            | 说明         |
  | --------------- | --------------- | ------------ |
  | associationId   | string          | 关联对象id   |
  | associationType | AssociationType | 关联对象类型 |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [{
        "id": "string",
        "url": "string",
        "token": "string",
        "triggers": ["NODE_CREATED"],
        "associationType": "REPO",
        "associationId": "string",
        "createdBy": "string",
        "createdDate": "string",
        "lastModifiedBy": "string",
        "lastModifiedDate": "string"
  	}],
    "traceId": null
  }
  ```

### 测试webhook

- API: GET /webhook/api/webhook/test/{id}

- API名称: test_webhook

- 功能说明：

    - 中文：测试webhook
    - English：test webhook

- 请求参数

  | 参数名 | 类型   | 说明       |
  | ------ | ------ | ---------- |
  | id     | string | webhook id |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
        "id": "string",
        "webHookUrl": "string",
        "triggeredEvent": "NODE_CREATED",
        "requestHeaders": {
            "key": "value"
        },
        "requestPayload": "string",
        "status": "SUCCESS" / "FAIL",
        "responseHeaders": {
            "key": "value"
        },
        "responseBody": "string",
        "requestDuration": 100,
        "requestTime": "2021-12-27T00:00:00.000",
        "errorMsg": "string"
    },
    "traceId": null
  }
  ```

### 重试webhook

- API: GET /webhook/api/webhook/retry/{logId}

- API名称: retry_webhook_request

- 功能说明：

    - 中文：重试webhook请求
    - English：retry webhook request

- 请求参数

  | 参数名 | 类型   | 说明                   |
  | ------ | ------ | ---------------------- |
  | logId  | string | webhook request log id |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": {
      "id": "string",
      "webHookUrl": "string",
      "triggeredEvent": "NODE_CREATED",
      "requestHeaders": {
          "key": "value"
      },
      "requestPayload": "string",
      "status": "SUCCESS",
      "responseHeaders": {
          "key": "value"
      },
      "responseBody": "string",
      "requestDuration": 100,
      "requestTime": "2021-12-27T00:00:00.000",
      "errorMsg": "string"
  },
  "traceId": null
}
```

### 查询webhook请求日志列表

- API: GET /webhook/api/log/list/{webHookId}

- API名称: list_webhook_log

- 功能说明：

    - 中文：查询webhook请求日志列表
    - English：list webhook log

- 请求参数

  | 参数名    | 类型   | 说明       |
  | --------- | ------ | ---------- |
  | webHookId | string | webhook id |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [{
      "webhookUrl": "string",
      "triggeredEvent": "NODE_UPLOADED",
      "requestHeaders": "string",
      "requestPayload": "string",
      "status": "SUCCESS",
      "responseHeaders": "string",
      "responseBody": "string",
      "requestDuration": "string",
      "requestTime": "string",
      "errorMsg": "string"
    }],
    "traceId": null
  }
  ```

