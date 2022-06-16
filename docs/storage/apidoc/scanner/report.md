# 扫描报告接口

[toc]

## 获取扫描报告预览

- API: POST /scanner/api/scan/reports/overview
- API 名称: get_report_overview
- 功能说明：
    - 中文：预览扫描报告
    - English：scan report overview
- 请求体

```json
{
	"scanner": "default",
	"credentialsKeyFiles": [
		{
			"credentialsKey": null,
			"sha256List":["af5d27f8921339531c5315c0928558f0de9ef1d27d07ff0f487602239cf885b5"]
		}	
	]
}
```

- 请求字段说明

| 字段                                      | 类型     | 是否必须 | 默认值 | 说明                      | Description     |
|-----------------------------------------|--------|------|-----|-------------------------|-----------------|
| scanner                                 | string | 是    | 无   | 要获取的报告使用的扫描器名称          | scanner name    |
| credentialsKeyFiles.credentialsKeyFiles | string | 否    | 无   | 被扫描文件所在存储,为null时表示在默认存储 | credentials key |
| credentialsKeyFiles.sha256List          | array  | 是    | 无   | 要查询报告的文件sha256列表        | sha256 list     |

- 响应体

```json
{
    "code": 0,
    "message": null,
    "data": [
        {
            "status": "SUCCESS",
            "sha256": "726683039c3af2005142c80af8ba823715ee07acbff17eb2662f08fb2b903d0f",
            "scanDate": "2022-03-10T17:06:05.122",
            "overview": {
                "cveLowCount": 5,
                "cveCriticalCount": 10,
                "licenseRiskLowCount": 52,
                "licenseRiskMidCount": 56,
                "sensitiveUriCount": 2095,
                "cveHighCount": 44,
                "sensitiveIpv4Count": 386,
                "licenseRiskNotAvailableCount": 30,
                "licenseRiskHighCount": 24,
                "cveMidCount": 35,
                "sensitiveEmailCount": 237
            }
        }
    ],
    "traceId": ""
}
```
- data字段说明

| 字段                           | 类型     | 说明                    | Description                         |
|------------------------------|--------|-----------------------|-------------------------------------|
| status                       | string | 文件扫描状态                | file scan status                    |
| sha256                       | string | 文件sha256              | file sha256                         |
| scanDate                     | string | 文件扫描时间                | file scan datetime                  |
| overview                     | object | 文件扫描结果预览，不同扫描器预览结果不一样 | file scan result overview           |
| cveLowCount                  | number | 低风险漏洞数                | low risk vulnerabilities count      |
| cveMidCount                  | number | 中风险漏洞数                | mid risk vulnerabilities count      |
| cveHighCount                 | number | 高风险漏洞数                | high risk vulnerabilities count     |
| cveCriticalCount             | number | 严重风险漏洞数               | critical risk vulnerabilities count |
| licenseRiskLowCount          | number | 制品依赖的库使用的低风险证书数量      | low risk license count              |
| licenseRiskMidCount          | number | 制品依赖的库使用的中风险证书数量      | mid risk license count              |
| licenseRiskHighCount         | number | 制品依赖的库使用的高风险证书数量      | high risk license count             |
| licenseRiskNotAvailableCount | number | 知识库未收录的证书数量           | unknown license count               |
| sensitiveEmailCount          | number | 敏感邮箱地址数               | sensitive email count               |
| sensitiveIpv4Count           | number | 敏感ipv4地址数             | sensitive ipv4 address count        |
| sensitiveIpv6Count           | number | 敏感ipv6地址数             | sensitive ipv6 address count        |
| sensitiveUriCount            | number | 敏感uri数                | sensitive uri count                 |
| sensitiveSecretCount         | number | 敏感密钥数                 | sensitive secret count              |
| sensitiveCryptoObjectCount   | number | 敏感密钥文件数               | sensitive crypto count              |

扫描结果预览字段参考[支持的扫描器](./supported-scanner.md)

## 获取扫描报告详情

- API: POST /scanner/api/scan/reports/detail/{projectId}/{repoName}/{artifactUri}
- API 名称: get_report_detail
- 功能说明：
  - 中文：获取扫描报告详情
  - English：scan report detail
- 请求体

```json
{
  "scanner": "default",
  "reportType": "SENSITIVE_ITEM",
  "pageLimit": {
    "pageNumber": 1,
    "pageSize": 10
  }
}
```

- 请求字段说明

| 字段                   | 类型     | 是否必须 | 默认值 | 说明                                                                                     | Description     |
|----------------------|--------|------|-----|----------------------------------------------------------------------------------------|-----------------|
| projectId            | string | 是    | 无   | 文件所属项目id                                                                               | project id      |
| repoName             | string | 是    | 无   | 文件所属仓库名                                                                                | repository name |
| artifactUri          | string | 是    | 无   | 文件路径                                                                                   | artifact uri    |
| scanner              | string | 是    | 无   | 扫描器名                                                                                   | scanner name    |
| reportType           | string | 是    | 无   | 扫描报告类型，arrowhead有CHECK_SEC_ITEM,APPLICATION_ITEM,CVE_SEC_ITEM,SENSITIVE_ITEM，4种类型的扫描报告 | report type     |
| pageLimit.pageNumber | number | 否    | 1   | 分页页码                                                                                   | page number     |
| pageLimit.pageSize   | number | 否    | 20  | 分页大小                                                                                   | page size       |

- 响应体

```json
{
  "code": 0,
  "message": null,
  "data": {
    "status": "SUCCESS",
    "sha256": "726683039c3af2005d4ac80af8ba823715ee07acbff17eb2662f08fb2b903d0f",
    "detail": {
      "pageNumber": 1,
      "pageSize": 10,
      "totalRecords": 1,
      "totalPages": 1,
      "records": [
        {
          "path": "test.xml",
          "type": "uri",
          "subtype": "uri",
          "content": "https://example.com",
          "domain": "example.com",
          "attr": {
            "scheme": "https"
          }
        }
      ],
      "count": 1,
      "page": 1
    },
    "type": "SENSITIVE_ITEM"
  },
  "traceId": ""
}
```

 data字段说明

| 字段                     | 类型     | 说明       | Description             |
|------------------------|--------|----------|-------------------------|
| status                 | string | 文件扫描状态   | file scan status        |
| sha256                 | string | 文件sha256 | file sha256             |
| detail.pageNumber      | number | 页码       | page number             |
| detail.pageSize        | number | 页大小      | page size               |
| detail.totalRecords    | number | 总记录数量    | total records           |
| detail.totalPage       | number | 总页数      | total page              |
| detail.records.path    | string | 制品用的库的路径 | lib path                |
| detail.records.type    | string | 敏感信息类型   | sensitive item class    |
| detail.records.subtype | string | 敏感信息子类型  | sensitive item subclass |
| detail.records.content | string | 敏感信息内容   | sensitive content       |
| detail.records.attr    | string | 敏感信息属性   | sensitive item attr     |

扫描结果详情字段参考[支持的扫描器](./supported-scanner.md)