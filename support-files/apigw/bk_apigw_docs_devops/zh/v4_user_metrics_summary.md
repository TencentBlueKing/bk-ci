### 请求方法/请求路径
#### GET /{apigwType}/v4/metrics/projectId/{projectId}/summary
### 资源描述
#### 获取看板 summary 数据
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| projectId | String | √   | 项目id |

#### Query参数
###### 无此参数
#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                    | 说明               |
| ------- | ------------------------------------------------------- | ---------------- |
| default | [ResultApigwMetricsSummary](#ResultApigwMetricsSummary) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "overview" : {
      "codeCheckInfo" : {
        "repoCodeccAvgScore" : "number",
        "resolvedDefectNum" : 0
      },
      "qualityInfo" : {
        "interceptionCount" : 0,
        "qualityInterceptionRate" : "number",
        "totalExecuteCount" : 0
      },
      "turboInfo" : {
        "turboSaveTime" : "number"
      }
    },
    "sumInfo" : {
      "pipelineSumInfoDO" : {
        "successExecuteCount" : 0,
        "totalAvgCostTime" : "number",
        "totalCostTime" : 0,
        "totalExecuteCount" : 0,
        "totalSuccessRate" : "number"
      }
    }
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultApigwMetricsSummary
##### 数据返回包装模型

| 参数名称    | 参数类型                                        | 必须  | 参数说明 |
| ------- | ------------------------------------------- | --- | ---- |
| data    | [ApigwMetricsSummary](#ApigwMetricsSummary) |     |      |
| message | string                                      |     | 错误信息 |
| status  | integer                                     | √   | 状态码  |

#### ApigwMetricsSummary
##### 流水线构建统计数据响应消息体

| 参数名称     | 参数类型                                                        | 必须  | 参数说明 |
| -------- | ----------------------------------------------------------- | --- | ---- |
| overview | [ThirdPlatformOverviewInfoVO](#ThirdPlatformOverviewInfoVO) |     |      |
| sumInfo  | [PipelineSumInfoVO](#PipelineSumInfoVO)                     |     |      |

#### ThirdPlatformOverviewInfoVO
##### 第三方平台度量概览

| 参数名称          | 参数类型                                | 必须  | 参数说明 |
| ------------- | ----------------------------------- | --- | ---- |
| codeCheckInfo | [CodeCheckInfoDO](#CodeCheckInfoDO) | √   |      |
| qualityInfo   | [QualityInfoDO](#QualityInfoDO)     | √   |      |
| turboInfo     | [TurboInfoDO](#TurboInfoDO)         | √   |      |

#### CodeCheckInfoDO
##### CodeCC度量信息

| 参数名称               | 参数类型    | 必须  | 参数说明           |
| ------------------ | ------- | --- | -------------- |
| repoCodeccAvgScore | number  |     | codecc检查代码库平均分 |
| resolvedDefectNum  | integer |     | 已解决缺陷数         |

#### QualityInfoDO
##### 质量红线度量信息

| 参数名称                    | 参数类型    | 必须  | 参数说明              |
| ----------------------- | ------- | --- | ----------------- |
| interceptionCount       | integer |     | 使用质量红线的流水线执行被拦截次数 |
| qualityInterceptionRate | number  |     | 质量红线拦截比例          |
| totalExecuteCount       | integer |     | 使用质量红线的流水线执行总次数   |

#### TurboInfoDO
##### 编译加速度量信息

| 参数名称          | 参数类型   | 必须  | 参数说明          |
| ------------- | ------ | --- | ------------- |
| turboSaveTime | number |     | 编译加速节省时间，单位：秒 |

#### PipelineSumInfoVO
##### 流水线汇总信息视图

| 参数名称              | 参数类型                                    | 必须  | 参数说明 |
| ----------------- | --------------------------------------- | --- | ---- |
| pipelineSumInfoDO | [PipelineSumInfoDO](#PipelineSumInfoDO) |     |      |

#### PipelineSumInfoDO
##### 流水线汇总信息

| 参数名称                | 参数类型    | 必须  | 参数说明       |
| ------------------- | ------- | --- | ---------- |
| successExecuteCount | integer | √   | 流水线成功执行次数  |
| totalAvgCostTime    | number  | √   | 流水线总平均执行耗时 |
| totalCostTime       | integer | √   | 流水线总执行耗时   |
| totalExecuteCount   | integer | √   | 流水线总执行次数   |
| totalSuccessRate    | number  | √   | 流水线总执行成功率  |

 
