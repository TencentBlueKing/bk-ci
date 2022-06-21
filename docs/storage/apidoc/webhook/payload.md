# WebHook事件消息载荷

## 公共消息

### 公共消息载荷

每个WebHook事件消息载荷都包含以下字段：

| 字段      | 类型   | 说明             |
| --------- | ------ | ---------------- |
| eventType | String | 触发事件类型     |
| user      | Object | 触发事件用户信息 |

### 公共请求头

| 名称           | 说明            |
| -------------- | --------------- |
| X-BKREPO-EVENT | 触发WebHook事件 |

## 测试事件

WebHook服务提供测试事件，方便用户添加WebHook后测试连通性

### 消息载荷

| 字段      | 类型   | 说明              |
| --------- | ------ | ----------------- |
| eventType | String | 触发事件类型      |
| user      | Object | 触发事件用户信息  |
| webHook   | Object | 测试的WebHook信息 |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "webHook" : {
    "id" : "string",
    "url" : "http://localhost",
    "triggers" : [ "NODE_CREATED", "NODE_DELETED" ],
    "associationType" : "ARTIFACT_REPO",
    "associationId" : "string",
    "createdBy" : "string",
    "createdDate" : "2021-09-13T17:45:33.878",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-13T17:45:33.879"
  },
  "eventType" : "WEBHOOK_TEST"
}
```

## 项目事件

### 创建项目

消息载荷

| 字段      | 类型   | 说明             |
| --------- | ------ | ---------------- |
| eventType | String | 触发事件类型     |
| user      | Object | 触发事件用户信息 |
| project   | Object | 创建的项目信息   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "project" : {
    "name" : "string",
    "displayName" : "http://localhost",
    "description" : "string",
    "createdBy" : "test",
    "createdDate" : "2021-09-13T17:45:33.878",
    "lastModifiedBy" : "test",
    "lastModifiedDate" : "2021-09-13T17:45:33.879"
  },
  "eventType" : "PROJECT_CREATED"
}
```



## 仓库事件

### 创建仓库

消息载荷

| 字段       | 类型   | 说明             |
| ---------- | ------ | ---------------- |
| eventType  | String | 触发事件类型     |
| user       | Object | 触发事件用户信息 |
| repository | Object | 创建的仓库信息   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "repository" : {
    "projectId" : "project",
    "name" : "repo",
    "type" : "GENERIC",
    "category" : "LOCAL",
    "public" : false,
    "description" : "string",
    "configuration" : "{}",
    "storageCredentialsKey" : "string",
    "createdBy" : "test",
    "createdDate" : "2021-09-13T17:45:33.878",
    "lastModifiedBy" : "test",
    "lastModifiedDate" : "2021-09-13T17:45:33.879"
  },
  "eventType" : "REPO_CREATED"
}
```



### 更新仓库

消息载荷

| 字段       | 类型   | 说明             |
| ---------- | ------ | ---------------- |
| eventType  | String | 触发事件类型     |
| user       | Object | 触发事件用户信息 |
| repository | Object | 创建的仓库信息   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "repository" : {
    "projectId" : "project",
    "name" : "repo",
    "type" : "GENERIC",
    "category" : "LOCAL",
    "public" : false,
    "description" : "string",
    "configuration" : "{}",
    "storageCredentialsKey" : "string",
    "createdBy" : "test",
    "createdDate" : "2021-09-13T17:45:33.878",
    "lastModifiedBy" : "test",
    "lastModifiedDate" : "2021-09-13T17:45:33.879"
  },
  "eventType" : "REPO_UPDATED"
}
```



### 删除仓库

消息载荷

| 字段      | 类型   | 说明                 |
| --------- | ------ | -------------------- |
| eventType | String | 触发事件类型         |
| user      | Object | 触发事件用户信息     |
| projectId | String | 删除仓库的所属项目ID |
| repoName  | String | 删除的仓库名称       |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "projectId" : "project",
  "repoName" : "repo",
  "eventType" : "REPO_DELETED"
}
```



## 节点事件

### 创建节点

消息载荷

| 字段      | 类型   | 说明             |
| --------- | ------ | ---------------- |
| eventType | String | 触发事件类型     |
| user      | Object | 触发事件用户信息 |
| node      | Object | 创建的节点信息   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "node" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "folder" : false,
    "path" : "/",
    "name" : "test.txt",
    "fullPath" : "/test.txt",
    "size" : 32,
    "sha256" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "md5" : "156c8805787b870939a80c708b64c946",
    "metadata" : { },
    "projectId" : "project",
    "repoName" : "repo"
  },
  "eventType" : "NODE_CREATED"
}
```

### 移动节点

消息载荷

| 字段         | 类型   | 说明             |
| ------------ | ------ | ---------------- |
| eventType    | String | 触发事件类型     |
| user         | Object | 触发事件用户信息 |
| node         | Object | 移动后的节点信息 |
| srcProjectId | String | 源项目Id         |
| srcRepoName  | String | 源仓库名         |
| srcFullPath  | String | 源节点完整路径   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "node" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "folder" : false,
    "path" : "/",
    "name" : "test.txt",
    "fullPath" : "/test.txt",
    "size" : 32,
    "sha256" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "md5" : "156c8805787b870939a80c708b64c946",
    "metadata" : { },
    "projectId" : "project",
    "repoName" : "repo"
  },
  "srcProjectId": "project",
  "srcRepoName": "repo",
  "srcFullPath": "/test.txt",
  "eventType" : "NODE_MOVED"
}
```

### 重命名节点

消息载荷

| 字段        | 类型   | 说明                   |
| ----------- | ------ | ---------------------- |
| eventType   | String | 触发事件类型           |
| user        | Object | 触发事件用户信息       |
| node        | Object | 重命名后的节点信息     |
| oldFullPath | String | 节点重命名前的完整路径 |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "node" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "folder" : false,
    "path" : "/",
    "name" : "test.txt",
    "fullPath" : "/test.txt",
    "size" : 32,
    "sha256" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "md5" : "156c8805787b870939a80c708b64c946",
    "metadata" : { },
    "projectId" : "project",
    "repoName" : "repo"
  },
  "oldFullPath": "/old.txt"
  "eventType" : "NODE_RENAMED"
}
```

### 复制节点

消息载荷

| 字段         | 类型   | 说明             |
| ------------ | ------ | ---------------- |
| eventType    | String | 触发事件类型     |
| user         | Object | 触发事件用户信息 |
| node         | Object | 复制后的节点信息 |
| srcProjectId | String | 源项目Id         |
| srcRepoName  | String | 源仓库名         |
| srcFullPath  | String | 源节点完整路径   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "node" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "folder" : false,
    "path" : "/",
    "name" : "test.txt",
    "fullPath" : "/test.txt",
    "size" : 32,
    "sha256" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "md5" : "156c8805787b870939a80c708b64c946",
    "metadata" : { },
    "projectId" : "project",
    "repoName" : "repo"
  },
  "srcProjectId": "project",
  "srcRepoName": "repo",
  "srcFullPath"
  "eventType" : "NODE_COPIED"
}
```

### 删除节点

消息载荷

| 字段      | 类型   | 说明             |
| --------- | ------ | ---------------- |
| eventType | String | 触发事件类型     |
| user      | Object | 触发事件用户信息 |
| node      | Object | 删除的节点信息   |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "projectId" : "project",
  "repoName" : "repo",
  "fullPath" : "/test.txt",
  "eventType" : "NODE_DELETED"
}
```

## 元数据事件

### 保存元数据

消息载荷

| 字段      | 类型   | 说明             |
| --------- | ------ | ---------------- |
| eventType | String | 触发事件类型     |
| user      | Object | 触发事件用户信息 |
| node      | Object | 节点信息         |
| metedata  | Object | 保存的元数据     |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "node" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "folder" : false,
    "path" : "/",
    "name" : "test.txt",
    "fullPath" : "/test.txt",
    "size" : 32,
    "sha256" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "md5" : "156c8805787b870939a80c708b64c946",
    "metadata" : { },
    "projectId" : "project",
    "repoName" : "repo"
  },
  "metedata" : { "key" : "value" }
  "eventType" : "METEDATA_SAVED"
}
```

### 删除元数据

消息载荷

| 字段                | 类型   | 说明             |
| ------------------- | ------ | ---------------- |
| eventType           | String | 触发事件类型     |
| user                | Object | 触发事件用户信息 |
| node                | Object | 节点信息         |
| deletedMetedataKeys | Set    | 删除的元数据Key  |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "node" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "folder" : false,
    "path" : "/",
    "name" : "test.txt",
    "fullPath" : "/test.txt",
    "size" : 32,
    "sha256" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "md5" : "156c8805787b870939a80c708b64c946",
    "metadata" : { },
    "projectId" : "project",
    "repoName" : "repo"
  },
  "deletedMetedataKeys": ["key1","key2"]
  "eventType" : "METEDATA_DELETED"
}
```

## 包版本事件

### 创建版本

消息载荷

| 字段           | 类型   | 说明             |
| -------------- | ------ | ---------------- |
| eventType      | String | 触发事件类型     |
| user           | Object | 触发事件用户信息 |
| packageVersion | Object | 包版本信息       |

示例

```json
{
  "user" : {
    "userId" : "string",
    "name" : "string",
    "email" : null,
    "phone" : null,
    "createdDate" : "2021-07-05T11:19:31.921",
    "locked" : false,
    "admin" : false
  },
  "packageVersion" : {
    "createdBy" : "string",
    "createdDate" : "2021-09-15T14:48:40.73",
    "lastModifiedBy" : "string",
    "lastModifiedDate" : "2021-09-15T14:48:40.73",
    "name" : false,
    "size" : "/",
    "downloads" : "test.txt",
    "stageTag" : "/test.txt",
    "metedata" : 32,
    "tags" : "28eb526a0b9e4a022cce7e9c6dffb11699c3c19a11b419d1b13873271a3c099e",
    "extension" : {"key": "value"},
    "contentPath" : "string"
  },
  "eventType" : "VERSION_CREATED"
}
```



