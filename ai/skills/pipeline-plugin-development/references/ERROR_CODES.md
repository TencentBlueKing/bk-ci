# 错误码规范

## 错误类型（errorType）

| 值 | 类型 | 说明 |
|----|------|------|
| 1 | USER | 用户配置错误（参数不合法等） |
| 2 | THIRD_PARTY | 第三方平台错误 |
| 3 | PLUGIN | 插件逻辑错误（默认） |

## 通用错误码（100 开头）

| 错误码 | 说明 | message 规范 |
|--------|------|--------------|
| 100001 | 输入参数非法 | 输入参数[xxx]非法：<具体要求> |
| 100002 | 网络连接超时 | 接口[xxx]，连接超时 |
| 100003 | 插件异常 | 未知的插件异常：<报错信息> |
| 100004 | 缺少必要参数 | 缺少必要参数xxx |
| 100400 | HTTP 400 | 接口[xxx]，返回码[400] |
| 100401 | HTTP 401 | 接口[xxx]，返回码[401] |
| 100403 | HTTP 403 | 接口[xxx]，返回码[403] |
| 100404 | HTTP 404 | 接口[xxx]，返回码[404] |
| 100500 | HTTP 500 | 接口[xxx]，返回码[500] |
| 100502 | HTTP 502 | 接口[xxx]，返回码[502] |

## 插件自定义错误码

以 **8** 开头，在插件代码库下增加 `error.json` 声明：

```json
[
    {
        "errorCode": 800001,
        "errorMsgZhCn": "配置文件不存在",
        "errorMsgEn": "Config file not found"
    },
    {
        "errorCode": 800002,
        "errorMsgZhCn": "编译失败",
        "errorMsgEn": "Build failed"
    }
]
```

## 第三方平台错误上报

当 `errorType=2` 时：

```json
{
    "errorType": 2,
    "errorCode": 106001,
    "platformCode": "tgit",
    "platformErrorCode": 500,
    "message": "调用工蜂接口失败：/api/v4/projects，返回码[500]"
}
```

### 已注册的第三方平台标识

| 平台 | 标识 | errorCode 前缀 |
|------|------|----------------|
| 蓝盾 | bkci | 101 |
| CodeCC | bkci_codecc | 102 |
| 制品库 | bkci_repo | 103 |
| 编译加速 | bkci_turbo | 104 |
| 作业平台 | bk_job | 105 |
| 工蜂 | tgit | 106 |
| TAPD | tapd | 107 |
| 智研 | zhiyan | 108 |
| 七彩石 | rainbow | 109 |
| 腾讯云 COS | cloud_cos | 110 |
