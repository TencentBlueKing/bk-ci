### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/artifactories/app_download_url
### 资源描述
#### 获取APP跳转链接
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称            | 参数类型                                      | 必须  | 参数说明   |
| --------------- | ----------------------------------------- | --- | ------ |
| artifactoryType | ENUM(PIPELINE, CUSTOM_DIR, IMAGE, REPORT) | √   | 版本仓库类型 |
| path            | String                                    | √   | 路径     |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                    | 说明               |
| ------- | ----------------------- | ---------------- |
| default | [ResultUrl](#ResultUrl) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?artifactoryType={artifactoryType}&path={path}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "url" : "",
    "url2" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultUrl
##### 数据返回包装模型

| 参数名称    | 参数类型        | 必须  | 参数说明 |
| ------- | ----------- | --- | ---- |
| data    | [Url](#Url) |     |      |
| message | string      |     | 错误信息 |
| status  | integer     | √   | 状态码  |

#### Url
##### 版本仓库-下载信息

| 参数名称 | 参数类型   | 必须  | 参数说明  |
| ---- | ------ | --- | ----- |
| url  | string | √   | 下载链接  |
| url2 | string |     | 下载链接2 |

 
