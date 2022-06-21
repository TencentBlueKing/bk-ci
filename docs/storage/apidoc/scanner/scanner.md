# 扫描器接口

[toc]

## 创建扫描器

- API: POST /scanner/api/scanners
- API 名称: create_scanner
- 功能说明：
    - 中文：创建扫描器
    - English：create scanner
- 创建arrowhead扫描器请求体

```json
{
  "name": "default",
  "version": "1::1",
  "type": "arrowhead",
  "rootPath": "/data1/arrowhead",
  "configFilePath": "/standalone.toml",
  "cleanWorkDir": false,
  "maxScanDuration": 600000,
  "knowledgeBase": {
    "secretId": "username",
    "secretKey": "key",
    "endpoint": "http://localhost:8021"
  },
  "container": {
    "image": "arrowhead:latest",
    "args": "/data/standalone.toml"
  },
  "resultFilterRule": {
    "sensitiveItemFilterRule": {
      "excludes": {
        "type": [
          "ipv6"
        ]
      }
    }
  }
}
```

- 请求字段说明
详情参考[支持的扫描器](./supported-scanner.md)

| 字段                                                | 类型      | 是否必须 | 默认值              | 说明                                               | Description                        |
|---------------------------------------------------|---------|------|------------------|--------------------------------------------------|------------------------------------|
| name                                              | string  | 是    | 无                | 扫描器名                                             | scanner name                       |
| version                                           | string  | 是    | 无                | 扫描器版本，arrowhead扫描器版本和漏洞库版本用::分隔                  | scanner version                    |
| type                                              | string  | 是    | 无                | 扫描器类型                                            | scanner type                       |
| rootPath                                          | string  | 是    | 无                | 扫描器工作根目录                                         | scanner work dir                   |
| configFilePath                                    | string  | 否    | /standalone.toml | 生成的arrowhead扫描器配置文件存放路径                          | arrowhead scanner config file path |
| cleanWorkDir                                      | boolean | 否    | true             | 扫描结束后是否清理目录                                      | clean work dir after scan          |
| maxScanDuration                                   | number  | 否    | 600000           | 扫描超时时间，单位为毫秒                                     | max scan duration                  |
| knowledgeBase.secretId                            | string  | 否    | 无                | 漏洞库用户名                                           | knowledge base username            |
| knowledgeBase.secretKey                           | string  | 否    | 无                | 漏洞库认证密钥                                          | knowledge base key                 |
| knowledgeBase.endpoint                            | string  | 否    | 无                | 漏洞库地址                                            | knowledge base address             |
| container.image                                   | string  | 是    | 无                | 使用的arrowhead镜像tag                                | arrowhead image tag                |
| container.args                                    | string  | 是    | 无                | 启动容器时传的参数                                        | container args                     |
| container.workDir                                 | string  | 否    | /data            | 容器内工作目录根目录                                       | work dir                           |
| container.inputDir                                | string  | 否    | /package         | 容器内扫描时的输入目录，相对于工作目录                              | input dir                          |
| container.outputDir                               | string  | 否    | /output          | 容器内扫描时的输出目录，相对于工作目录                              | output dir                         |
| resultFilterRule.sensitiveItemFilterRule.excludes | array   | 否    | 无                | 敏感信息扫描结果过滤规则，key为敏感信息结果字段名，value为要过滤的敏感信息结果字段值列表 | sensitiv item filter rule          |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": {
    "name": "default",
    "version": "1::1",
    "rootPath": "/data1/arrowhead",
    "configFilePath": "/standalone.toml",
    "cleanWorkDir": false,
    "knowledgeBase": {
      "secretId": "username",
      "secretKey": "key",
      "endpoint": "http://localhost:8021"
    },
    "container": {
      "image": "arrowhead:latest",
      "args": "/data/standalone.toml",
      "workDir": "/data",
      "inputDir": "/package",
      "outputDir": "/output"
    },
    "resultFilterRule": {
      "sensitiveItemFilterRule": {
        "excludes": {
          "type": [
            "ipv6"
          ]
        }
      }
    },
    "maxScanDuration": 600000,
    "type": "arrowhead"
  },
  "traceId": ""
}
 ```

## 查询扫描器

- API: GET /scanner/api/scanners/{scannerName}
- API 名称: get_scanner
- 功能说明：
    - 中文：查询扫描器
    - English：get scanner
- 请求体 此接口请求体为空
- 请求字段说明

| 字段          | 类型     | 是否必须 | 默认值 | 说明   | Description  |
|-------------|--------|------|-----|------|--------------|
| scannerName | string | 否    | 无   | 扫描器名 | scanner name |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": {
    "name": "default",
    "version": "1::1",
    "rootPath": "/data1/arrowhead",
    "configFilePath": "/standalone.toml",
    "cleanWorkDir": false,
    "knowledgeBase": {
      "secretId": "username",
      "secretKey": "key",
      "endpoint": "http://localhost:8021"
    },
    "container": {
      "image": "arrowhead:latest",
      "args": "/data/standalone.toml",
      "workDir": "/data",
      "inputDir": "/package",
      "outputDir": "/output"
    },
    "resultFilterRule": {
      "sensitiveItemFilterRule": {
        "excludes": {
          "type": [
            "ipv6"
          ]
        }
      }
    },
    "maxScanDuration": 600000,
    "type": "arrowhead"
  },
  "traceId": null
}
 ```

- data字段说明
详情参考[支持的扫描器](./supported-scanner.md)

## 删除扫描器

- API: DELETE /scanner/api/scanners/{scannerName}
- API 名称: delete_scanner
- 功能说明：
    - 中文：删除扫描器
    - English：delete scanner
- 请求体 此接口请求体为空

- 请求字段说明

| 字段          | 类型     | 是否必须 | 默认值 | 说明    | Description  |
|-------------|--------|------|-----|-------|--------------|
| scannerName | string | 是    | 无   | 扫描器名称 | scanner name |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": null,
  "traceId": null
}
 ```

## 获取扫描器列表

- API: GET /scanner/api/scanners
- API 名称: list_scanner
- 功能说明：
    - 中文：获取扫描器列表
    - English：list scanner
- 请求体 此接口请求体为空

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": [
    {
      "name": "default",
      "version": "1::1",
      "rootPath": "/data1/arrowhead",
      "configFilePath": "/standalone.toml",
      "cleanWorkDir": false,
      "knowledgeBase": {
        "secretId": "username",
        "secretKey": "key",
        "endpoint": "http://localhost:8021"
      },
      "container": {
        "image": "arrowhead:latest",
        "args": "/data/standalone.toml",
        "workDir": "/data",
        "inputDir": "/package",
        "outputDir": "/output"
      },
      "resultFilterRule": {
        "sensitiveItemFilterRule": {
          "excludes": {
            "type": [
              "ipv6"
            ]
          }
        }
      },
      "maxScanDuration": 600000,
      "type": "arrowhead"
    }
  ],
  "traceId": ""
}
```

- data字段说明

详情参考[支持的扫描器](./supported-scanner.md)