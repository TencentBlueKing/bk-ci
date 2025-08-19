### 请求方法/请求路径
#### GET /{apigwType}/v4/projects/{projectId}/quality/rule
### 资源描述
#### 获取拦截规则列表
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明        |
| --------- | ------ | --- | ----------- |
| apigwType | String | √   | apigw Type  |
| projectId | String | √   | 项目ID(项目英文名) |

#### Query参数

| 参数名称     | 参数类型    | 必须  | 参数说明              |
| -------- | ------- | --- | ----------------- |
| page     | integer |     | 页目                |
| pageSize | integer |     | 每页条数(默认20, 最大100) |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                                                                      | 说明               |
| ------- | ----------------------------------------------------------------------------------------- | ---------------- |
| default | [ResultPageQualityRuleSummaryWithPermission](#ResultPageQualityRuleSummaryWithPermission) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?page={page}&pageSize={pageSize}' \
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
      "controlPoint" : {
        "cnName" : "",
        "hashId" : "",
        "name" : ""
      },
      "enable" : false,
      "gatewayId" : "",
      "indicatorList" : [ {
        "cnName" : "",
        "hashId" : "",
        "name" : "",
        "operation" : "",
        "threshold" : ""
      } ],
      "interceptTimes" : 0,
      "name" : "",
      "permissions" : {
        "canDelete" : false,
        "canEdit" : false,
        "canEnable" : false
      },
      "pipelineCount" : 0,
      "pipelineExecuteCount" : 0,
      "range" : "enum",
      "rangeSummary" : [ {
        "id" : "",
        "lackElements" : [ "" ],
        "name" : "",
        "type" : ""
      } ],
      "ruleHashId" : ""
    } ],
    "totalPages" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPageQualityRuleSummaryWithPermission
##### 数据返回包装模型

| 参数名称    | 参数类型                                                                          | 必须  | 参数说明 |
| ------- | ----------------------------------------------------------------------------- | --- | ---- |
| data    | [PageQualityRuleSummaryWithPermission](#PageQualityRuleSummaryWithPermission) |     |      |
| message | string                                                                        |     | 错误信息 |
| status  | integer                                                                       | √   | 状态码  |

#### PageQualityRuleSummaryWithPermission
##### 分页数据包装模型

| 参数名称       | 参数类型                                                                        | 必须  | 参数说明  |
| ---------- | --------------------------------------------------------------------------- | --- | ----- |
| count      | integer                                                                     | √   | 总记录行数 |
| page       | integer                                                                     | √   | 第几页   |
| pageSize   | integer                                                                     | √   | 每页多少条 |
| records    | List<[QualityRuleSummaryWithPermission](#QualityRuleSummaryWithPermission)> | √   | 数据    |
| totalPages | integer                                                                     | √   | 总共多少页 |

#### QualityRuleSummaryWithPermission
##### 质量红线-规则简要信息v2

| 参数名称                 | 参数类型                                                | 必须  | 参数说明             |
| -------------------- | --------------------------------------------------- | --- | ---------------- |
| controlPoint         | [RuleSummaryControlPoint](#RuleSummaryControlPoint) | √   |                  |
| enable               | boolean                                             | √   | 是否启用             |
| gatewayId            | string                                              |     | 红线ID             |
| indicatorList        | List<[RuleSummaryIndicator](#RuleSummaryIndicator)> | √   | 指标列表             |
| interceptTimes       | integer                                             | √   | 拦截次数             |
| name                 | string                                              | √   | 规则名称             |
| permissions          | [RulePermission](#RulePermission)                   | √   |                  |
| pipelineCount        | integer                                             | √   | 流水线个数            |
| pipelineExecuteCount | integer                                             | √   | 生效流水线执次数         |
| range                | ENUM(ANY, PART_BY_TAG, PART_BY_NAME)                | √   | 生效范围             |
| rangeSummary         | List<[RuleRangeSummary](#RuleRangeSummary)>         | √   | 包含模板和流水线的生效范围（新） |
| ruleHashId           | string                                              | √   | 规则HashId         |

#### RuleSummaryControlPoint
##### 控制点

| 参数名称   | 参数类型   | 必须  | 参数说明 |
| ------ | ------ | --- | ---- |
| cnName | string |     |      |
| hashId | string |     |      |
| name   | string |     |      |

#### RuleSummaryIndicator
##### 指标列表

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| cnName    | string |     |      |
| hashId    | string |     |      |
| name      | string |     |      |
| operation | string |     |      |
| threshold | string |     |      |

#### RulePermission
##### 质量红线-规则权限

| 参数名称      | 参数类型    | 必须  | 参数说明     |
| --------- | ------- | --- | -------- |
| canDelete | boolean | √   | 是否可删除    |
| canEdit   | boolean | √   | 是否可编辑    |
| canEnable | boolean | √   | 是否可停用/启用 |

#### RuleRangeSummary
##### 包含模板和流水线的生效范围（新）

| 参数名称         | 参数类型         | 必须  | 参数说明 |
| ------------ | ------------ | --- | ---- |
| id           | string       |     |      |
| lackElements | List<string> |     |      |
| name         | string       |     |      |
| type         | string       |     |      |

 
