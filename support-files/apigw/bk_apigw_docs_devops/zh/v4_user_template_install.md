### 请求方法/请求路径
#### POST /{apigwType}/v4/market/template_install_from_store
### 资源描述
#### 安装研发商店模板到项目
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数

| 参数名称             | 参数类型                                      | 必须   |
| ---------------- | ----------------------------------------- | ---- |
| 安装研发商店模板到项目请求报文体 | [InstallTemplateReq](#InstallTemplateReq) | true |

#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X POST '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### POST 请求样例

```Json
{
  "projectCodeList" : [ "" ],
  "templateCode" : ""
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
#### InstallTemplateReq
##### 安装模板到项目请求报文

| 参数名称            | 参数类型         | 必须  | 参数说明 |
| --------------- | ------------ | --- | ---- |
| projectCodeList | List<string> | √   | 项目标识 |
| templateCode    | string       | √   | 模板代码 |

#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
