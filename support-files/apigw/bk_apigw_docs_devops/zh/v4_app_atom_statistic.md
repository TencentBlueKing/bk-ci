### 请求方法/请求路径
#### GET /{apigwType}/v4/atoms/atom_statistic
### 资源描述
#### 根据插件代码获取插件统计信息
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

| HTTP代码  | 参数类型                                          | 说明               |
| ------- | --------------------------------------------- | ---------------- |
| default | [ResultStoreStatistic](#ResultStoreStatistic) | default response |

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
    "commentCnt" : 0,
    "downloads" : 0,
    "hotFlag" : false,
    "pipelineCnt" : 0,
    "recentActiveDuration" : "number",
    "recentExecuteNum" : 0,
    "score" : "number",
    "successRate" : "number"
  },
  "message" : "",
  "status" : 0
}
```

### 相关模型数据
#### ResultStoreStatistic
##### 数据返回包装模型

| 参数名称    | 参数类型                              | 必须  | 参数说明 |
| ------- | --------------------------------- | --- | ---- |
| data    | [StoreStatistic](#StoreStatistic) |     |      |
| message | string                            |     | 错误信息 |
| status  | integer                           | √   | 状态码  |

#### StoreStatistic
##### 统计信息

| 参数名称                 | 参数类型    | 必须  | 参数说明         |
| -------------------- | ------- | --- | ------------ |
| commentCnt           | integer | √   | 评论量          |
| downloads            | integer | √   | 下载量          |
| hotFlag              | boolean |     | 是否为受欢迎组件     |
| pipelineCnt          | integer |     | 流水线个数        |
| recentActiveDuration | number  |     | 最近活跃时长，单位：小时 |
| recentExecuteNum     | integer |     | 最近执行次数       |
| score                | number  |     | 星级评分         |
| successRate          | number  |     | 成功率          |

 
