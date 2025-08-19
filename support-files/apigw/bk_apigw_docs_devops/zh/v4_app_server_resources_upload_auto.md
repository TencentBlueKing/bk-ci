### 请求方法/请求路径
#### GET /{apigwType}/v4/turbo/triggerAutoUpload/{month}
### 资源描述
#### 触发项目资源统计上报任务
### 输入参数说明
#### Path参数

| 参数名称  | 参数类型   | 必须  | 参数说明   |
| ----- | ------ | --- | ------ |
| month | String | √   | 所属周期月份 |

#### Query参数

| 参数名称      | 参数类型   | 必须  | 参数说明   |
| --------- | ------ | --- | ------ |
| endDate   | String |     | 截止统计日期 |
| startDate | String |     | 起始统计日期 |

#### Header参数

| 参数名称                | 参数类型   | 必须  | 参数说明             |
| ------------------- | ------ | --- | ---------------- |
| Content-Type        | string | √   | application/json |
| X-DEVOPS-UID        | string | √   | 用户名              |
| X-DEVOPS-PROJECT-ID | String |     | 项目id             |

#### Body参数
###### 无此参数
#### 响应参数

| HTTP代码  | 参数类型                                | 说明               |
| ------- | ----------------------------------- | ---------------- |
| default | [ResponseBoolean](#ResponseBoolean) | default response |

### Curl 请求样例

```Json
curl -X GET '[请替换为上方API地址栏请求地址]?endDate={endDate}&startDate={startDate}' \
-H 'Content-Type: application/json' \
-H 'X-DEVOPS-UID: 用户名' \
-H 'X-DEVOPS-PROJECT-ID: 项目id' 
```

### default 返回样例

```Json
{
  "code" : 0,
  "data" : false,
  "message" : ""
}
```

### 相关模型数据
#### ResponseBoolean
##### 

| 参数名称    | 参数类型    | 必须  | 参数说明 |
| ------- | ------- | --- | ---- |
| code    | integer |     |      |
| data    | boolean |     |      |
| message | string  |     |      |

 
