### 请求方法/请求路径
#### GET /{apigwType}/v4/atoms/atom_info
### 资源描述
#### 根据插件代码获取插件详细信息
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |

#### Query参数

| 参数名称     | 参数类型   | 必须  | 参数说明 |
| -------- | ------ | --- | ---- |
| atomCode | String | √   | 插件代码 |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                    | 说明               |
| ------- | --------------------------------------- | ---------------- |
| default | [ResultAtomVersion](#ResultAtomVersion) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?atomCode={atomCode}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "atomCode" : "",
    "atomId" : "",
    "atomStatus" : "",
    "atomType" : "",
    "category" : "",
    "classifyCode" : "",
    "classifyName" : "",
    "codeSrc" : "",
    "createTime" : "",
    "creator" : "",
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
    "defaultFlag" : false,
    "description" : "",
    "docsLink" : "",
    "editFlag" : false,
    "flag" : false,
    "frontendType" : "enum",
    "honorInfos" : [ {
      "createTime" : "string",
      "honorId" : "",
      "honorName" : "",
      "honorTitle" : "",
      "mountFlag" : false
    } ],
    "htmlTemplateVersion" : "",
    "indexInfos" : [ {
      "description" : "",
      "hover" : "",
      "iconUrl" : "",
      "indexCode" : "",
      "indexLevelName" : "",
      "indexName" : ""
    } ],
    "initProjectCode" : "",
    "jobType" : "",
    "labelList" : [ {
      "createTime" : 0,
      "id" : "",
      "labelCode" : "",
      "labelName" : "",
      "labelType" : "",
      "updateTime" : 0
    } ],
    "language" : "",
    "logoUrl" : "",
    "modifier" : "",
    "name" : "",
    "os" : [ "" ],
    "privateReason" : "",
    "projectCode" : "",
    "publisher" : "",
    "recommendFlag" : false,
    "releaseType" : "",
    "repositoryAuthorizer" : "",
    "summary" : "",
    "updateTime" : "",
    "userCommentInfo" : {
      "commentFlag" : false,
      "commentId" : ""
    },
    "version" : "",
    "versionContent" : "",
    "visibilityLevel" : "",
    "yamlFlag" : false
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultAtomVersion
##### 数据返回包装模型

| 参数名称    | 参数类型                        | 必须  | 参数说明 |
| ------- | --------------------------- | --- | ---- |
| data    | [AtomVersion](#AtomVersion) |     |      |
| message | string                      |     | 错误信息 |
| status  | integer                     | √   | 状态码  |

#### AtomVersion
##### 数据

| 参数名称                 | 参数类型                                              | 必须  | 参数说明                                      |
| -------------------- | ------------------------------------------------- | --- | ----------------------------------------- |
| atomCode             | string                                            |     | 插件标识                                      |
| atomId               | string                                            |     | 插件ID                                      |
| atomStatus           | string                                            | √   | 插件状态                                      |
| atomType             | string                                            |     | 插件类型                                      |
| category             | string                                            |     | 插件范畴                                      |
| classifyCode         | string                                            |     | 插件分类code                                  |
| classifyName         | string                                            |     | 插件分类名称                                    |
| codeSrc              | string                                            |     | 代码库链接                                     |
| createTime           | string                                            |     | 创建时间                                      |
| creator              | string                                            |     | 创建人                                       |
| dailyStatisticList   | List<[StoreDailyStatistic](#StoreDailyStatistic)> |     | 每日统计信息列表                                  |
| defaultFlag          | boolean                                           |     | 是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件 |
| description          | string                                            |     | 插件描述                                      |
| docsLink             | string                                            |     | 插件说明文档链接                                  |
| editFlag             | boolean                                           |     | 是否可编辑                                     |
| flag                 | boolean                                           |     | 是否可安装标识                                   |
| frontendType         | ENUM(HISTORY, NORMAL, SPECIAL)                    |     | 前端UI渲染方式                                  |
| honorInfos           | List<[HonorInfo](#HonorInfo)>                     |     | 荣誉信息                                      |
| htmlTemplateVersion  | string                                            |     | 前端渲染模板版本（1.0代表历史存量插件渲染模板版本）               |
| indexInfos           | List<[StoreIndexInfo](#StoreIndexInfo)>           |     | 指标信息列表                                    |
| initProjectCode      | string                                            |     | 插件的初始化项目                                  |
| jobType              | string                                            |     | 适用Job类型                                   |
| labelList            | List<[Label](#Label)>                             |     | 标签列表                                      |
| language             | string                                            |     | 开发语言                                      |
| logoUrl              | string                                            |     | logo地址                                    |
| modifier             | string                                            |     | 修改人                                       |
| name                 | string                                            |     | 插件名称                                      |
| os                   | List<string>                                      |     | 操作系统                                      |
| privateReason        | string                                            |     | 插件代码库不开源原因                                |
| projectCode          | string                                            |     | 插件的调试项目                                   |
| publisher            | string                                            |     | 发布者                                       |
| recommendFlag        | boolean                                           |     | 是否推荐标识 true：推荐，false：不推荐                  |
| releaseType          | string                                            |     | 发布类型                                      |
| repositoryAuthorizer | string                                            |     | 插件代码库授权者                                  |
| summary              | string                                            |     | 插件简介                                      |
| updateTime           | string                                            |     | 修改时间                                      |
| userCommentInfo      | [StoreUserCommentInfo](#StoreUserCommentInfo)     |     |                                           |
| version              | string                                            |     | 版本号                                       |
| versionContent       | string                                            |     | 版本日志                                      |
| visibilityLevel      | string                                            |     | 项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源     |
| yamlFlag             | boolean                                           |     | yaml可用标识 true：是，false：否                   |

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

#### Label
##### 标签信息

| 参数名称       | 参数类型    | 必须  | 参数说明                                           |
| ---------- | ------- | --- | ---------------------------------------------- |
| createTime | integer | √   | 创建日期                                           |
| id         | string  | √   | 标签ID                                           |
| labelCode  | string  | √   | 标签代码                                           |
| labelName  | string  | √   | 标签名称                                           |
| labelType  | string  | √   | 类别 ATOM:插件 TEMPLATE:模板 IMAGE:镜像 IDE_ATOM:IDE插件 |
| updateTime | integer | √   | 更新日期                                           |

#### StoreUserCommentInfo
##### 用户评论信息

| 参数名称        | 参数类型    | 必须  | 参数说明                 |
| ----------- | ------- | --- | -------------------- |
| commentFlag | boolean | √   | 是否已评论 true:是，false:否 |
| commentId   | string  |     | 评论ID                 |

 
