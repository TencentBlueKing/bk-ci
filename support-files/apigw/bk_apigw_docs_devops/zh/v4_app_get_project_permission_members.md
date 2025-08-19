### 请求方法/请求路径
#### GET /{apigwType}/v4/auth/project/{projectId}/getResourceGroupUsers
### 资源描述
#### 获取项目权限分组成员
           该接口是一个可以查多种权限名单的接口，这取决于resourceType。
           示例①：查询A项目下p-B流水线拥有者有哪些，如果group为null，则会取有p-B流水线相关权限的所有人。
               - projectId: A
               - resourceType: PIPELINE_DEFAULT
               - resourceCode: p-B
               - group: RESOURCE_MANAGER
           示例②：查询A项目管理员有哪些,如果group为null，则A项目下所有人。
               - projectId: A
               - resourceType: PROJECT
               - resourceCode: A
               - group: MANAGER
        
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型   | 必须  | 参数说明       |
| --------- | ------ | --- | ---------- |
| apigwType | String | √   | apigw Type |
| projectId | String | √   | 项目Code     |

#### Query参数

| 参数名称         | 参数类型                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | 必须  | 参数说明    |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ------- |
| group        | ENUM(VISITOR, CIADMIN, MANAGER, DEVELOPER, MAINTAINER, TESTER, PM, QC, CI_MANAGER, GRADE_ADMIN, CGS_MANAGER, RESOURCE_MANAGER, EDITOR, EXECUTOR, VIEWER)                                                                                                                                                                                                                                                                                                                                    |     | 资源用户组类型 |
| resourceCode | String                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |     | 资源code  |
| resourceType | ENUM(BCS_DEV_IMAGE, BCS_PROD_IMAGE, CODE_REPERTORY, PIPELINE_DEFAULT, PIPELINE_GROUP, PIPELINE_TEMPLATE, ARTIFACTORY_CUSTOM_DIR, TICKET_CREDENTIAL, TICKET_CERT, ENVIRONMENT_ENVIRONMENT, ENVIRONMENT_ENV_NODE, EXPERIENCE_TASK, EXPERIENCE_TASK_NEW, EXPERIENCE_GROUP, EXPERIENCE_GROUP_NEW, SCAN_TASK, QUALITY_RULE, QUALITY_GROUP, QUALITY_GROUP_NEW, WETEST_TASK, WETEST_EMAIL_GROUP, PROJECT, CGS, TURBO, CODECC_TASK, CODECC_IGNORE_TYPE, CODECC_RULE_SET, SCC_TASK, SCC_SCAN_SCHEMA) |     | 资源类型    |

#### Header参数

| 参数名称         | 参数类型   | 必须  | 参数说明             |
| ------------ | ------ | --- | ---------------- |
| Content-Type | string | √   | application/json |
| X-DEVOPS-UID | string | √   | 用户名              |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                  | 说明               |
| ------- | ------------------------------------- | ---------------- |
| default | [ResultListString](#ResultListString) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?group={group}&resourceCode={resourceCode}&resourceType={resourceType}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' 
```

### default 返回样例

```Json
{
  "data" : [ "" ],
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultListString
##### 数据返回包装模型

| 参数名称    | 参数类型         | 必须  | 参数说明 |
| ------- | ------------ | --- | ---- |
| data    | List<string> |     | 数据   |
| message | string       |     | 错误信息 |
| status  | integer      | √   | 状态码  |

 
