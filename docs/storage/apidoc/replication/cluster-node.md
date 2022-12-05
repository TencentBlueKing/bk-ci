# Replication集群节点管理接口

[toc]

## 创建集群节点

- API: POST /replication/api/cluster/create
- API 名称: create_cluster_node
- 功能说明：
  - 中文：创建集群节点
  - English：create cluster node
- 请求体

  ```json
  {
      "name": "shanghai",
      "url": "http://bkrepo.xxx.com",
      "certificate": null,
      "username": "username",
      "password": "password",
      "type": "EDGE"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|节点名称，长度不超过32位|cluster node name|
  |url|string|是|无|节点url，符合URL统一规范|cluster node url|
  |certificate|string|否|null|节点证书|cluster node certificate|
  |username|string|否|null|节点认证用户名|cluster node username|
  |password|string|否|null|节点认证密码|cluster node password|
  |type|string|是|否|[CENTER,EDGE,STANDALONE]|cluster node type|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 测试集群连通状态

- API: GET /replication/api/cluster/tryConnect
- API 名称: test_connect
- 功能说明：
	- 中文：测试集群连通状态
	- English：test connect status

- 请求体:

  ``` json
  {
      "name":"shanghai"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|节点名称|cluster node name|

- 响应体:

  ```
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```
  
## 删除集群节点

- API: DELETE /replication/api/cluster/delete/{id}
- API 名称: delete_cluster_node
- 功能说明：
  - 中文：删除集群节点
  - English：delete cluster node
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |id|string|是|无|节点唯一id|cluster node id|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 校验集群节点是否存在

- API: GET /replication/api/cluster/exist?name=shanghai
- API 名称: check_cluster_exist
- 功能说明：
  - 中文：校验集群节点是否存在
  - English：check cluster node exist
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|集群节点名称|cluster node name|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": true,
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |data|boolean|集群节点是否存在|cluster node exist or not|

## 根据id查询集群节点信息

- API: GET /replication/api/cluster/info/{id}
- API 名称: get_cluster_node_info
- 功能说明：
  - 中文：查询集群节点详情
  - English：get cluster node info
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |id|string|是|无|集群id|cluster node id|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "id":"609a57ddba727966d138c51e",
      "name":"shanghai",
      "status":"HEALTHY",
      "errorReason":null,
      "type":"EDGE",
      "url":"http://backup.bkrepo.xxx.com",
      "username":"admin",
      "password":"password",
      "certificate":null,
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
  |id|string|集群节点id|cluster node id|
  |name|string|集群节点名称|cluster node name|
  |status|enum|[HEALTHY,UNHEALTHY]|cluster node status|
  |errorReason|string/集群状态问题错误原因|cluster node status failed reason|
  |type|string|集群节点类型|cluster node type|
  |url|string|集群节点url|cluster node url|
  |username|string|集群节点用户名|cluster node username|
  |password|string|集群节点密码|cluster node password|
  |certificate|string|集群节点证书|cluster node certificate|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
  
## 根据name查询集群节点信息

- API: GET /replication/api/cluster/info?name=shanghai
- API 名称: get_cluster_node_info
- 功能说明：
  - 中文：查询集群节点详情
  - English：get cluster node info
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|集群节点名称|cluster node name|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "id":"609a57ddba727966d138c51e",
      "name":"shanghai",
      "status":"HEALTHY",
      "errorReason":null,
      "type":"EDGE",
      "url":"http://backup.bkrepo.xxx.com",
      "username":"admin",
      "password":"password",
      "certificate":null,
      "createdBy" : "system",
      "createdDate" : "2020-03-16T12:13:03.371",
      "lastModifiedBy" : "system",
      "lastModifiedDate" : "2020-03-16T12:13:03.371"
    },
    "traceId": null
  }
  ```

- data字段说明

  同上
  
## 获取中心节点

- API: GET /replication/api/cluster/info/center
- API 名称: get_center_cluster_node_info
- 功能说明：
  - 中文：查询集群节点详情
  - English：get center cluster node info
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|集群节点名称|cluster node name|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": {
      "id":"609a57ddba727966d138c51e",
      "name":"shanghai",
      "status":"HEALTHY",
      "errorReason":null,
      "type":"CENTER",
      "url":"http://backup.bkrepo.xxx.com",
      "username":"admin",
      "password":"password",
      "certificate":null,
      "createdBy" : "system",
      "createdDate" : "2020-03-16T12:13:03.371",
      "lastModifiedBy" : "system",
      "lastModifiedDate" : "2020-03-16T12:13:03.371"
    },
    "traceId": null
  }
  ```

- data字段说明

  同上
  
## 查询边缘节点列表

- API: GET /replication/api/cluster/list/edge
- API 名称: get_edge_cluster_node_list
- 功能说明：
  - 中文：查询集群节点详情
  - English：get edge cluster node list
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|集群节点名称|cluster node name|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
      {
        "id":"609a57ddba727966d138c51e",
        "name":"shanghai",
        "status":"HEALTHY",
        "errorReason":null,
        "type":"CENTER",
        "url":"http://backup.bkrepo.xxx.com",
        "username":"admin",
        "password":"password",
        "certificate":null,
        "createdBy" : "system",
        "createdDate" : "2020-03-16T12:13:03.371",
        "lastModifiedBy" : "system",
        "lastModifiedDate" : "2020-03-16T12:13:03.371"
      }
    ],
    "traceId": null
  }
  ```

- data字段说明

  同上

## 列表查询集群节点

- API: GET /replication/api/cluster/list?name=shanghai&type=EDGE
- API 名称: list_cluster_node
- 功能说明：
  - 中文：列表查询集群节点
  - English：list cluster node
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|否|无|集群节点名称|cluster name|
  |type|string|否|无|集群节点类型，枚举值|cluster type|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
      {
      	"id":"609a57ddba727966d138c51e",
      	"name":"shanghai",
      	"status":"HEALTHY",
      	"errorReason":null,
      	"type":"EDGE",
      	"url":"http://backup.bkrepo.xxx.com",
      	"username":"admin",
      	"password":"password",
      	"certificate":null,
      	"createdBy" : "system",
        "createdDate" : "2020-03-16T12:13:03.371",
        "lastModifiedBy" : "system",
        "lastModifiedDate" : "2020-03-16T12:13:03.371"
      }
    ],
    "traceId": null
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|集群节点id|cluster node id|
  |name|string|集群节点名称|cluster node name|
  |status|enum|[HEALTHY,UNHEALTHY]|cluster node status|
  |errorReason|string/集群状态问题错误原因|cluster node status failed reason|
  |type|string|集群节点类型|cluster node type|
  |url|string|集群节点url|cluster node url|
  |username|string|集群节点用户名|cluster node username|
  |password|string|集群节点密码|cluster node password|
  |certificate|string|集群节点证书|cluster node certificate|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|

## 分页列表查询集群节点

- API: GET /replication/api/cluster/page?name=shanghai&type=EDGE
- API 名称: list_cluster_node_page
- 功能说明：
  - 中文：分页列表查询集群节点
  - English：list cluster node page
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |pageNumber|int|是|无|当前页|page number|
  |pageSize|int|是|无|分页数量|page size|
  |name|string|否|无|集群节点名称|cluster name|
  |type|string|否|无|[EDGE,CENTER,STANDALONE]|cluster type|

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
      	  "id":"609a57ddba727966d138c51e",
      	  "name":"shanghai",
          "status":"HEALTHY",
          "errorReason":null,
          "type":"EDGE",
          "url":"http://backup.bkrepo.xxx.com",
          "username":"admin",
          "password":"password",
          "certificate":null,
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

  |字段|类型|说明|Description|
  |---|---|---|---|
  |id|string|集群节点id|cluster node id|
  |name|string|集群节点名称|cluster node name|
  |status|enum|[HEALTHY,UNHEALTHY]|cluster node status|
  |errorReason|string/集群状态问题错误原因|cluster node status failed reason|
  |type|string|集群节点类型|cluster node type|
  |url|string|集群节点url|cluster node url|
  |username|string|集群节点用户名|cluster node username|
  |password|string|集群节点密码|cluster node password|
  |certificate|string|集群节点证书|cluster node certificate|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
