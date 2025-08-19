### 请求方法/请求路径
#### POST /{apigwType}/v4/atoms/install_atom
### 资源描述
#### 安装插件到项目
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数

| 参数名称        | 参数类型                                                       | 必须  | 参数说明 |
| ----------- | ---------------------------------------------------------- | --- | ---- |
| channelCode | ENUM(BS, AM, CODECC, GCLOUD, GIT, GONGFENGSCAN, CODECC_EE) |     | 渠道类型 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称         | 参数类型                              | 必须   |
| ------------ | --------------------------------- | ---- |
| 安装插件到项目请求报文体 | [InstallAtomReq](#InstallAtomReq) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]?channelCode={channelCode}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "atomCode" : "",
  "projectCode" : [ "" ]
}
```

### default 返回样例

```Json
{
  "data" : false,
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### InstallAtomReq
##### 安装插件到项目请求报文

| 参数名称        | 参数类型         | 必须  | 参数说明 |
| ----------- | ------------ | --- | ---- |
| atomCode    | string       | √   | 插件标识 |
| projectCode | List<string> | √   | 项目标识 |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
