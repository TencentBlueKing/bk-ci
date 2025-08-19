### 请求方法/请求路径
#### GET /{apigwType}/v4/atoms/atom_search
### 资源描述
#### 获取所有流水线插件信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数

| 参数名称          | 参数类型                                                                                | 必须  | 参数说明                     |
| ------------- | ----------------------------------------------------------------------------------- | --- | ------------------------ |
| classifyCode  | String                                                                              |     | 插件分类                     |
| keyword       | String                                                                              |     | 搜索关键字                    |
| labelCode     | String                                                                              |     | 功能标签                     |
| page          | integer                                                                             |     | 页码                       |
| pageSize      | integer                                                                             |     | 每页数量                     |
| qualityFlag   | boolean                                                                             |     | 是否有红线指标                  |
| rdType        | ENUM(0, 1)                                                                          |     | 研发来源                     |
| recommendFlag | boolean                                                                             |     | 是否推荐标识 true：推荐，false：不推荐 |
| score         | integer                                                                             |     | 评分                       |
| sortType      | ENUM(NAME, CREATE_TIME, UPDATE_TIME, PUBLISHER, DOWNLOAD_COUNT, RECENT_EXECUTE_NUM) |     | 排序                       |
| yamlFlag      | boolean                                                                             |     | yaml是否可用                 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                          | 说明               |
| ------- | --------------------------------------------- | ---------------- |
| default | [ResultMarketAtomResp](#ResultMarketAtomResp) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?classifyCode={classifyCode}&keyword={keyword}&labelCode={labelCode}&page={page}&pageSize={pageSize}&qualityFlag={qualityFlag}&rdType={rdType}&recommendFlag={recommendFlag}&score={score}&sortType={sortType}&yamlFlag={yamlFlag}' \
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
      "buildLessRunFlag" : false,
      "category" : "",
      "classifyCode" : "",
      "code" : "",
      "dailyStatisticList" : [ {
        "dailyActiveDuration" : "number",
        "dailyDownloads" : 0,
        "dailyFailDetail" : {
          "string" : "Any 任意类型，参照实际请求或返回"
        },
        "dailyFailNum" : 0,
        "dailyFailRate" : "number",
        "dailySuccessNum" : 0,
        "dailySuccessRate" : "number",
        "statisticsTime" : "",
        "totalDownloads" : 0
      } ],
      "docsLink" : "",
      "downloads" : 0,
      "extData" : {
        "string" : "Any 任意类型，参照实际请求或返回"
      },
      "flag" : false,
      "honorInfos" : [ {
        "createTime" : "string",
        "honorId" : "",
        "honorName" : "",
        "honorTitle" : "",
        "mountFlag" : false
      } ],
      "hotFlag" : false,
      "id" : "",
      "indexInfos" : [ {
        "description" : "",
        "hover" : "",
        "iconUrl" : "",
        "indexCode" : "",
        "indexLevelName" : "",
        "indexName" : ""
      } ],
      "installed" : false,
      "logoUrl" : "",
      "modifier" : "",
      "name" : "",
      "os" : [ "" ],
      "publicFlag" : false,
      "publisher" : "",
      "rdType" : "",
      "recentExecuteNum" : 0,
      "recommendFlag" : false,
      "score" : "number",
      "status" : "",
      "summary" : "",
      "type" : "",
      "updateFlag" : false,
      "updateTime" : "",
      "version" : "",
      "yamlFlag" : false
    } ]
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultMarketAtomResp
##### 数据返回包装模型

| 参数名称    | 参数类型                              | 必须  | 参数说明 |
| ------- | --------------------------------- | --- | ---- |
| data    | [MarketAtomResp](#MarketAtomResp) |     |      |
| message | string                            |     | 错误信息 |
| status  | integer                           | √   | 状态码  |

#### MarketAtomResp
##### 插件市场搜索插件返回报文

| 参数名称     | 参数类型                            | 必须  | 参数说明   |
| -------- | ------------------------------- | --- | ------ |
| count    | integer                         | √   | 总记录数   |
| page     | integer                         |     | 当前页码值  |
| pageSize | integer                         |     | 每页记录大小 |
| records  | List<[MarketItem](#MarketItem)> | √   | 数据集合   |

#### MarketItem
##### 研发商店组件信息

| 参数名称               | 参数类型                                              | 必须  | 参数说明                     |
| ------------------ | ------------------------------------------------- | --- | ------------------------ |
| buildLessRunFlag   | boolean                                           |     | 无编译环境组件是否可以在编译环境下执行标识    |
| category           | string                                            |     | 所属范畴，多个范畴标识用逗号分隔         |
| classifyCode       | string                                            |     | 分类                       |
| code               | string                                            | √   | 组件标识                     |
| dailyStatisticList | List<[StoreDailyStatistic](#StoreDailyStatistic)> |     | 每日统计信息列表                 |
| docsLink           | string                                            |     | 帮助文档                     |
| downloads          | integer                                           |     | 下载量                      |
| extData            | Map<String, Any>                                  |     | 扩展字段集合                   |
| flag               | boolean                                           | √   | 是否有权限安装标识                |
| honorInfos         | List<[HonorInfo](#HonorInfo)>                     |     | 荣誉信息列表                   |
| hotFlag            | boolean                                           |     | 是否为受欢迎组件                 |
| id                 | string                                            | √   | ID                       |
| indexInfos         | List<[StoreIndexInfo](#StoreIndexInfo)>           |     | 指标信息列表                   |
| installed          | boolean                                           |     | 是否已在该项目安装 true：是，false：否 |
| logoUrl            | string                                            |     | logo链接                   |
| modifier           | string                                            | √   | 修改人                      |
| name               | string                                            | √   | 名称                       |
| os                 | List<string>                                      |     | 支持的操作系统                  |
| publicFlag         | boolean                                           | √   | 是否公共标识                   |
| publisher          | string                                            | √   | 发布者                      |
| rdType             | string                                            |     | 研发来源类型                   |
| recentExecuteNum   | integer                                           |     | 最近执行次数                   |
| recommendFlag      | boolean                                           |     | 是否推荐标识 true：推荐，false：不推荐 |
| score              | number                                            |     | 评分                       |
| status             | string                                            | √   | 状态                       |
| summary            | string                                            |     | 简介                       |
| type               | string                                            | √   | 组件类型                     |
| updateFlag         | boolean                                           |     | 是否需要更新                   |
| updateTime         | string                                            | √   | 修改时间                     |
| version            | string                                            | √   | 版本号                      |
| yamlFlag           | boolean                                           |     | yaml可用标识 true：是，false：否  |

#### StoreDailyStatistic
##### 每日统计信息

| 参数名称                | 参数类型             | 必须  | 参数说明                       |
| ------------------- | ---------------- | --- | -------------------------- |
| dailyActiveDuration | number           |     | 每日活跃时长，单位：小时               |
| dailyDownloads      | integer          | √   | 每日下载量                      |
| dailyFailDetail     | Map<String, Any> |     | 每日执行失败详情                   |
| dailyFailNum        | integer          | √   | 每日执行失败数                    |
| dailyFailRate       | number           |     | 每日执行失败率                    |
| dailySuccessNum     | integer          | √   | 每日执行成功数                    |
| dailySuccessRate    | number           |     | 每日执行成功率                    |
| statisticsTime      | string           | √   | 统计时间，格式yyyy-MM-dd HH:mm:ss |
| totalDownloads      | integer          | √   | 总下载量                       |

#### HonorInfo
##### 荣誉信息

| 参数名称       | 参数类型    | 必须  | 参数说明 |
| ---------- | ------- | --- | ---- |
| createTime | string  | √   | 创建时间 |
| honorId    | string  | √   | 荣誉ID |
| honorName  | string  | √   | 荣誉名称 |
| honorTitle | string  | √   | 荣誉头衔 |
| mountFlag  | boolean | √   | 是否佩戴 |

#### StoreIndexInfo
##### 研发商店指标信息

| 参数名称           | 参数类型   | 必须  | 参数说明   |
| -------------- | ------ | --- | ------ |
| description    | string | √   | 指标描述   |
| hover          | string | √   | 指标状态显示 |
| iconUrl        | string | √   | 图标地址   |
| indexCode      | string | √   | 指标代码   |
| indexLevelName | string | √   | 等级名称   |
| indexName      | string | √   | 指标名称   |

 
