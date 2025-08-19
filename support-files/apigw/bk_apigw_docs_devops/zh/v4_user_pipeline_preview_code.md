### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/version/preview_code
### 资源描述
#### 触发前配置
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目ID       |

#### Query参数

| 参数名称       | 参数类型    | 必须  | 参数说明   |
| ---------- | ------- | --- | ------ |
| pipelineId | String  | √   | 流水线id  |
| version    | integer |     | 流水线版本号 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                            | 说明               |
| ------- | ----------------------------------------------- | ---------------- |
| default | [ResultPreviewResponse](#ResultPreviewResponse) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?pipelineId={pipelineId}&version={version}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "notice" : [ {
      "endMark" : {
        "column" : 0,
        "line" : 0
      },
      "startMark" : {
        "column" : 0,
        "line" : 0
      }
    } ],
    "pipeline" : [ {
      "endMark" : {
        "column" : 0,
        "line" : 0
      },
      "startMark" : {
        "column" : 0,
        "line" : 0
      }
    } ],
    "setting" : [ {
      "endMark" : {
        "column" : 0,
        "line" : 0
      },
      "startMark" : {
        "column" : 0,
        "line" : 0
      }
    } ],
    "trigger" : [ {
      "endMark" : {
        "column" : 0,
        "line" : 0
      },
      "startMark" : {
        "column" : 0,
        "line" : 0
      }
    } ],
    "yaml" : ""
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPreviewResponse
##### 数据返回包装模型

| 参数名称    | 参数类型                                | 必须  | 参数说明 |
| ------- | ----------------------------------- | --- | ---- |
| data    | [PreviewResponse](#PreviewResponse) |     |      |
| message | string                              |     | 错误信息 |
| status  | integer                             | √   | 状态码  |

#### PreviewResponse
##### 流水线 yaml 带定位信息

| 参数名称     | 参数类型                                | 必须  | 参数说明   |
| -------- | ----------------------------------- | --- | ------ |
| notice   | List<[TransferMark](#TransferMark)> |     | 通知配置   |
| pipeline | List<[TransferMark](#TransferMark)> |     | 流水线编排  |
| setting  | List<[TransferMark](#TransferMark)> |     | 基础设置   |
| trigger  | List<[TransferMark](#TransferMark)> |     | 触发器配置  |
| yaml     | string                              | √   | yaml内容 |

#### TransferMark
##### 互转yaml定位

| 参数名称      | 参数类型          | 必须  | 参数说明 |
| --------- | ------------- | --- | ---- |
| endMark   | [Mark](#Mark) | √   |      |
| startMark | [Mark](#Mark) | √   |      |

#### Mark
##### 标记尾

| 参数名称   | 参数类型    | 必须  | 参数说明   |
| ------ | ------- | --- | ------ |
| column | integer |     | 列数 0开始 |
| line   | integer |     | 行数 0开始 |

 
