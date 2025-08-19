### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/export
### 资源描述
#### 导出流水线模板
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称        | 参数类型    | 必须  | 参数说明    |
| ----------- | ------- | --- | ------- |
| pipelineId  | String  | √   | 流水线Id   |
| storageType | String  |     | 导出的数据类型 |
| version     | integer |     | 导出的目标版本 |

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
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}&storageType={storageType}&version={version}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{ }
```

### 相关模型数据
 
