### 请求方法/请求路径
#### GET /{apigwType}/v4/auth/project/{projectId}/{groupId}/get_group_permission_detail
### 资源描述
#### 查询用户组权限详情
### 输入参数说明
#### Path参数

| 参数名称      | 参数类型    | 必须  | 参数说明       |
| --------- | ------- | --- | ---------- |
| apigwType | String  | √   | apigw Type |
| groupId   | integer | √   | 用户组ID      |
| projectId | String  | √   | projectId  |

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

| HTTP代码  | 参数类型                                                                                      | 说明               |
| ------- | ----------------------------------------------------------------------------------------- | ---------------- |
| default | [ResultMapStringListGroupPermissionDetailVo](#ResultMapStringListGroupPermissionDetailVo) | default response |

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
    "string" : [ {
      "actionId" : "",
      "actionRelatedResourceType" : "",
      "name" : "",
      "relatedResourceInfos" : [ {
        "instance" : [ [ {
          "id" : "",
          "name" : "",
          "system_id" : "",
          "type" : "",
          "type_name" : ""
        } ] ],
        "name" : "",
        "type" : ""
      } ]
    } ]
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultMapStringListGroupPermissionDetailVo
##### 数据返回包装模型

| 参数名称    | 参数类型                                                                   | 必须  | 参数说明 |
| ------- | ---------------------------------------------------------------------- | --- | ---- |
| data    | Map<String, List<[GroupPermissionDetailVo](#GroupPermissionDetailVo)>> |     | 数据   |
| message | string                                                                 |     | 错误信息 |
| status  | integer                                                                | √   | 状态码  |

#### GroupPermissionDetailVo
##### 组权限详情

| 参数名称                      | 参数类型                                              | 必须  | 参数说明      |
| ------------------------- | ------------------------------------------------- | --- | --------- |
| actionId                  | string                                            | √   | 操作id      |
| actionRelatedResourceType | string                                            | √   | 操作关联的资源类型 |
| name                      | string                                            | √   | 操作名       |
| relatedResourceInfos      | List<[RelatedResourceInfo](#RelatedResourceInfo)> | √   | 关联资源      |

#### RelatedResourceInfo
##### 组权限详情

| 参数名称     | 参数类型                                            | 必须  | 参数说明  |
| -------- | ----------------------------------------------- | --- | ----- |
| instance | List<List<[InstancePathDTO](#InstancePathDTO)>> | √   | 资源实例  |
| name     | string                                          | √   | 资源类型名 |
| type     | string                                          | √   | 资源类型  |

#### InstancePathDTO
##### 资源实例

| 参数名称      | 参数类型   | 必须  | 参数说明 |
| --------- | ------ | --- | ---- |
| id        | string |     |      |
| name      | string |     |      |
| system_id | string |     |      |
| type      | string |     |      |
| type_name | string |     |      |

 
