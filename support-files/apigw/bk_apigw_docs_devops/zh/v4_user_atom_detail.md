### 请求方法/请求路径
#### GET /{apigwType}/v4/atoms/atom_detail
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
| version  | String | √   | 版本号  |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                      | 说明               |
| ------- | ----------------------------------------- | ---------------- |
| default | [ResultPipelineAtom](#ResultPipelineAtom) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?atomCode={atomCode}&version={version}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : {
    "atomCode" : "",
    "atomLabelList" : [ {
      "createTime" : 0,
      "id" : "",
      "labelCode" : "",
      "labelName" : "",
      "labelType" : "",
      "updateTime" : 0
    } ],
    "atomStatus" : "",
    "atomType" : "",
    "buildLessRunFlag" : false,
    "category" : "",
    "classType" : "",
    "classifyCode" : "",
    "classifyId" : "",
    "classifyName" : "",
    "createTime" : 0,
    "creator" : "",
    "data" : {
      "string" : "Any 任意类型，参照实际请求或返回"
    },
    "defaultFlag" : false,
    "description" : "",
    "docsLink" : "",
    "frontendType" : "enum",
    "htmlTemplateVersion" : "",
    "icon" : "",
    "id" : "",
    "jobType" : "",
    "latestFlag" : false,
    "logoUrl" : "",
    "name" : "",
    "os" : [ "" ],
    "props" : {
      "string" : "Any 任意类型，参照实际请求或返回"
    },
    "recommendFlag" : false,
    "serviceScope" : [ "" ],
    "summary" : "",
    "updateTime" : 0,
    "version" : "",
    "versionList" : [ {
      "versionName" : "",
      "versionValue" : ""
    } ],
    "visibilityLevel" : "",
    "weight" : 0
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultPipelineAtom
##### 数据返回包装模型

| 参数名称    | 参数类型                          | 必须  | 参数说明 |
| ------- | ----------------------------- | --- | ---- |
| data    | [PipelineAtom](#PipelineAtom) |     |      |
| message | string                        |     | 错误信息 |
| status  | integer                       | √   | 状态码  |

#### PipelineAtom
##### 流水线-流水线插件信息

| 参数名称                | 参数类型                              | 必须  | 参数说明                                                     |
| ------------------- | --------------------------------- | --- | -------------------------------------------------------- |
| atomCode            | string                            | √   | 插件代码                                                     |
| atomLabelList       | List<[Label](#Label)>             |     | 插件标签列表                                                   |
| atomStatus          | string                            | √   | 插件状态                                                     |
| atomType            | string                            |     | 插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发                 |
| buildLessRunFlag    | boolean                           |     | 无构建环境插件是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以                 |
| category            | string                            |     | 插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件                         |
| classType           | string                            | √   | 插件大类（插件市场发布的插件分为有marketBuild：构建环境和marketBuildLess：无构建环境） |
| classifyCode        | string                            |     | 所属插件分类代码                                                 |
| classifyId          | string                            |     | 所属插件分类Id                                                 |
| classifyName        | string                            |     | 所属插件分类名称                                                 |
| createTime          | integer                           | √   | 插件创建时间                                                   |
| creator             | string                            | √   | 创建人                                                      |
| data                | Map<String, Any>                  |     | 预留字段（设置规则等信息的json串）                                      |
| defaultFlag         | boolean                           |     | 是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件                |
| description         | string                            |     | 插件描述                                                     |
| docsLink            | string                            |     | 插件说明文档链接                                                 |
| frontendType        | ENUM(HISTORY, NORMAL, SPECIAL)    |     | 前端UI渲染方式                                                 |
| htmlTemplateVersion | string                            |     | 前端渲染模板版本（1.0代表历史存量插件渲染模板版本）                              |
| icon                | string                            |     | 插件图标                                                     |
| id                  | string                            | √   | 插件ID                                                     |
| jobType             | string                            |     | 适用Job类型，AGENT： 编译环境，AGENT_LESS：无编译环境                     |
| latestFlag          | boolean                           |     | 是否为最新版本插件 true：最新 false：非最新                              |
| logoUrl             | string                            |     | 插件logo                                                   |
| name                | string                            | √   | 插件名称                                                     |
| os                  | List<string>                      |     | 支持的操作系统                                                  |
| props               | Map<String, Any>                  |     | 自定义扩展容器前端表单属性字段的Json串                                    |
| recommendFlag       | boolean                           |     | 是否推荐标识 true：推荐，false：不推荐                                 |
| serviceScope        | List<string>                      |     | 服务范围                                                     |
| summary             | string                            |     | 插件简介                                                     |
| updateTime          | integer                           | √   | 插件最后修改时间                                                 |
| version             | string                            | √   | 版本号                                                      |
| versionList         | List<[VersionInfo](#VersionInfo)> | √   | 版本信息列表                                                   |
| visibilityLevel     | string                            |     | 项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源                    |
| weight              | integer                           |     | 权重（数值越大代表权重越高）                                           |

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

#### VersionInfo
##### 商店组件-版本信息

| 参数名称         | 参数类型   | 必须  | 参数说明 |
| ------------ | ------ | --- | ---- |
| versionName  | string | √   | 版本名称 |
| versionValue | string | √   | 版本值  |

 
