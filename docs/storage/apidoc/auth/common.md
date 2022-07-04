# auth服务api调用认证

 auth服务认证有提级，区别于通用协议。


## 公共请求头

### 平台账号，优先
|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|Authorization|string|是|无|自定义Auth认证头，Platform base64(accessKey:secretKey)|Platform Auth header|
|X-BKREPO-UID|string|是|无|实际操作用户|operate user|

### 系统admin账号
|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|Authorization|string|是|无|Basic Auth认证头，Basic base64(username:password)|Basic Auth header|





