### 请求方法/请求路径
#### DELETE /{apigwType}/v4/projects/{projectId}/pipelineView
### 资源描述
#### 删除视图(流水线组)
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   | 项目ID |

#### Query参数

| 参数名称      | 参数类型    | 必须  | 参数说明                                    |
| --------- | ------- | --- | --------------------------------------- |
| isProject | boolean |     | 维度是否为项目,和viewName搭配使用                   |
| viewId    | String  |     | 用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入 |
| viewName  | String  |     | 用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                            | 说明               |
| ------- | ------------------------------- | ---------------- |
| default | [ResultBoolean](#ResultBoolean) | default response |

### Curl 请求样例

```Json
curl -X DELETE '[请替换为上方API地址栏请求地址]?isProject={isProject}&viewId={viewId}&viewName={viewName}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
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
#### ResultBoolean
##### 数据返回包装模型

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| data    | boolean |     | 数据   |
| message | string  |     | 错误信息 |
| status  | integer | √   | 状态码  |

 
