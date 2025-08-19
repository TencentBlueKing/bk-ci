### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/quality/intercept_list
### 资源描述
#### 获取拦截记录
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称            | 参数类型                                                                          | 必须  | 参数说明              |
| --------------- | ----------------------------------------------------------------------------- | --- | ----------------- |
| endTime         | integer                                                                       |     | 截止时间              |
| interceptResult | ENUM(PASS, FAIL, WAIT, INTERCEPT, INTERCEPT_PASS, INTERCEPT_TIMEOUT, UNCHECK) |     | 状态                |
| page            | integer                                                                       |     | 页号                |
| pageSize        | integer                                                                       |     | 每页条数(默认20, 最大100) |
| pipelineId      | String                                                                        |     | 流水线ID             |
| ruleHashId      | String                                                                        |     | 规则ID              |
| startTime       | integer                                                                       |     | 开始时间              |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                              | 说明               |
| ------- | ----------------------------------------------------------------- | ---------------- |
| default | [ResultPageRuleInterceptHistory](#ResultPageRuleInterceptHistory) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?endTime={endTime}&interceptResult={interceptResult}&page={page}&pageSize={pageSize}&pipelineId={pipelineId}&ruleHashId={ruleHashId}&startTime={startTime}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "count" : 0,
    "page" : 0,
    "pageSize" : 0,
    "records" : [ {
      "buildId" : "",
      "buildNo" : "",
      "checkTimes" : 0,
      "hashId" : "",
      "interceptList" : [ {
        "actualValue" : "",
        "controlPoint" : "",
        "controlPointElementId" : "",
        "detail" : "",
        "indicatorId" : "",
        "indicatorName" : "",
        "indicatorType" : "",
        "logPrompt" : "",
        "operation" : "enum",
        "pass" : false,
        "value" : ""
      } ],
      "interceptResult" : "enum",
      "num" : 0,
      "pipelineId" : "",
      "pipelineIsDelete" : false,
      "pipelineName" : "",
      "qualityRuleBuildHisOpt" : {
        "gateKeepers" : [ "" ],
        "gateOptTime" : "",
        "gateOptUser" : "",
        "ruleHashId" : "",
        "stageId" : ""
      },
      "remark" : "",
      "ruleHashId" : "",
      "ruleName" : "",
      "timestamp" : 0
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageRuleInterceptHistory
##### 数据返回包装模型

| 参数名称    | 参数类型                                                  | 必须  | 参数说明 |
| ------- | ----------------------------------------------------- | --- | ---- |
| data    | [PageRuleInterceptHistory](#PageRuleInterceptHistory) |     |      |
| message | string                                                |     | 错误信息 |
| status  | integer                                               | √   | 状态码  |

#### PageRuleInterceptHistory
##### 分页数据包装模型

| 参数名称       | 参数类型                                                | 必须  | 参数说明  |
| ---------- | --------------------------------------------------- | --- | ----- |
| count      | integer                                             | √   | 总记录行数 |
| page       | integer                                             | √   | 第几页   |
| pageSize   | integer                                             | √   | 每页多少条 |
| records    | List<[RuleInterceptHistory](#RuleInterceptHistory)> | √   | 数据    |
| totalPages | integer                                             | √   | 总共多少页 |

#### RuleInterceptHistory
##### 质量红线-拦截记录

| 参数名称                   | 参数类型                                                                          | 必须  | 参数说明                          |
| ---------------------- | ----------------------------------------------------------------------------- | --- | ----------------------------- |
| buildId                | string                                                                        | √   | 构建ID                          |
| buildNo                | string                                                                        | √   | 构建号                           |
| checkTimes             | integer                                                                       | √   | 检查次数                          |
| hashId                 | string                                                                        | √   | hashId 红线拦截记录在表中主键Id的哈希值，是唯一的 |
| interceptList          | List<[QualityRuleInterceptRecord](#QualityRuleInterceptRecord)>               |     | 描述列表                          |
| interceptResult        | ENUM(PASS, FAIL, WAIT, INTERCEPT, INTERCEPT_PASS, INTERCEPT_TIMEOUT, UNCHECK) | √   | 拦截结果                          |
| num                    | integer                                                                       | √   | 项目里的序号                        |
| pipelineId             | string                                                                        | √   | 流水线ID                         |
| pipelineIsDelete       | boolean                                                                       | √   | 流水线是否已删除                      |
| pipelineName           | string                                                                        | √   | 流水线名称                         |
| qualityRuleBuildHisOpt | [QualityRuleBuildHisOpt](#QualityRuleBuildHisOpt)                             |     |                               |
| remark                 | string                                                                        | √   | 描述                            |
| ruleHashId             | string                                                                        | √   | 规则HashId                      |
| ruleName               | string                                                                        | √   | 规则名称                          |
| timestamp              | integer                                                                       | √   | 时间戳(秒)                        |

#### QualityRuleInterceptRecord
##### 质量红线-拦截规则拦截记录

| 参数名称                  | 参数类型                     | 必须  | 参数说明     |
| --------------------- | ------------------------ | --- | -------- |
| actualValue           | string                   |     | 实际值      |
| controlPoint          | string                   | √   | 控制点      |
| controlPointElementId | string                   |     | 控制点的插件id |
| detail                | string                   |     | 指标详情     |
| indicatorId           | string                   | √   | 指标ID     |
| indicatorName         | string                   | √   | 指标名称     |
| indicatorType         | string                   |     | 指标插件类型   |
| logPrompt             | string                   |     | 指标日志输出详情 |
| operation             | ENUM(GT, GE, LT, LE, EQ) | √   | 关系       |
| pass                  | boolean                  | √   | 是否通过     |
| value                 | string                   |     | 阈值值大小    |

#### QualityRuleBuildHisOpt
##### 质量红线-把关操作记录

| 参数名称        | 参数类型         | 必须  | 参数说明     |
| ----------- | ------------ | --- | -------- |
| gateKeepers | List<string> |     | 红线把关人    |
| gateOptTime | string       |     | 操作时间     |
| gateOptUser | string       |     | 操作人      |
| ruleHashId  | string       | √   | 红线hashId |
| stageId     | string       |     | stageId  |

 
