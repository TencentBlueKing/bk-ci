### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/logs/download_logs
### 资源描述
#### 下载日志接口(注意: 接口返回application/octet-stream数据，Request Header Accept 类型不一致将导致错误)
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称            | 参数类型    | 必须  | 参数说明                     |
| --------------- | ------- | --- | ------------------------ |
| archiveFlag     | boolean |     | 是否查询归档数据                 |
| buildId         | String  | √   | 构建ID (b-开头)              |
| containerHashId | String  |     | 对应containerHashId (c-开头) |
| executeCount    | integer |     | 执行次数                     |
| jobId           | String  |     | 对应jobId                  |
| pipelineId      | String  |     | 流水线ID (p-开头)             |
| stepId          | String  |     | 对应stepId                 |
| tag             | String  |     | 对应element ID (e-开头)      |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型 | 说明               |
| ------- | ---- | ---------------- |
| default |      | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?archiveFlag={archiveFlag}&buildId={buildId}&containerHashId={containerHashId}&executeCount={executeCount}&jobId={jobId}&pipelineId={pipelineId}&stepId={stepId}&tag={tag}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{ }
```

### 相关模型数据
 
